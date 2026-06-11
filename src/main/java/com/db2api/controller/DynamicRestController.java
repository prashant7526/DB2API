package com.db2api.controller;

import com.db2api.persistent.api.ApiDefinition;
import com.db2api.service.api.ApiDefinitionService;
import com.db2api.service.api.SchemaDiscoveryService;
import com.db2api.service.connection.ExternalConnectivityService;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.SQLExec;
import org.apache.cayenne.query.SQLSelect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Pattern;

/**
 * REST Controller that handles dynamic API requests.
 * It translates incoming HTTP requests (GET, PUT, DELETE) into SQL queries
 * executed against external databases based on ApiDefinitions.
 */
@RestController
@RequestMapping("/api/dynamic")
public class DynamicRestController {

    private static final Logger logger = LoggerFactory.getLogger(DynamicRestController.class);

    /** Pattern for valid SQL identifiers (table names, column names). */
    private static final Pattern VALID_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final ApiDefinitionService apiDefinitionService;
    private final ExternalConnectivityService externalConnectivityService;
    private final SchemaDiscoveryService schemaDiscoveryService;

    /**
     * Constructs the DynamicRestController with required services.
     * 
     * @param apiDefinitionService        the service for retrieving API mappings
     * @param externalConnectivityService the service for external database
     *                                    connectivity
     * @param schemaDiscoveryService      the service for discovering database schemas
     */
    public DynamicRestController(ApiDefinitionService apiDefinitionService,
            ExternalConnectivityService externalConnectivityService,
            SchemaDiscoveryService schemaDiscoveryService) {
        this.apiDefinitionService = apiDefinitionService;
        this.externalConnectivityService = externalConnectivityService;
        this.schemaDiscoveryService = schemaDiscoveryService;
    }

    /**
     * Validates that a given identifier (table or column name) is safe to use in
     * SQL by checking it against an allowlist from the database schema.
     *
     * @param identifier the identifier to validate
     * @param allowedSet the set of valid identifiers
     * @return true if the identifier is safe, false otherwise
     */
    private boolean isAllowedIdentifier(String identifier, Set<String> allowedSet) {
        if (identifier == null || !VALID_IDENTIFIER.matcher(identifier).matches()) {
            return false;
        }
        return allowedSet.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(identifier));
    }

    /**
     * Retrieves data from a dynamic API endpoint.
     * 
     * @param tableName the name of the table to query
     * @return a list of records from the external database
     */
    @GetMapping("/{tableName}")
    public ResponseEntity<List<Map<String, Object>>> getData(@PathVariable String tableName) {
        ApiDefinition apiDef = apiDefinitionService.getApiDefinitionByTableNameAndType(tableName, "REST");
        if (apiDef == null) {
            return ResponseEntity.notFound().build();
        }

        if (!apiDef.getAllowedOperations().contains("GET")) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        // Use the admin-configured table name from the API definition instead of
        // the raw path variable to prevent SQL injection
        String safeTableName = apiDef.getTableName();

        try {
            ObjectContext context = externalConnectivityService.getContext(apiDef.getConnection());

            // Build column list from API definition (validated against schema)
            Set<String> schemaColumns = new HashSet<>(
                    schemaDiscoveryService.getColumns(apiDef.getConnection(), safeTableName));
            String columns = buildColumnList(apiDef.getIncludedColumns(), schemaColumns);
            String sql = "SELECT " + columns + " FROM " + safeTableName;

            List<DataRow> rows = SQLSelect.dataRowQuery(sql).select(context);

            // Convert DataRows to List<Map<String, Object>>
            List<Map<String, Object>> results = new ArrayList<>();
            for (DataRow row : rows) {
                results.add(new HashMap<>(row));
            }

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Error fetching data from table {}", safeTableName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Updates data in a dynamic API endpoint.
     *
     * @param tableName  the name of the table to update
     * @param data       the data to be updated
     * @param conditions the WHERE clause conditions for the update
     * @return a success or error response
     */
    @PutMapping("/{tableName}")
    public ResponseEntity<?> updateData(@PathVariable String tableName,
            @RequestBody Map<String, Object> data,
            @RequestParam Map<String, String> conditions) {
        ApiDefinition apiDef = apiDefinitionService.getApiDefinitionByTableNameAndType(tableName, "REST");
        if (apiDef == null || !apiDef.getAllowedOperations().contains("PUT")) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        // Use the admin-configured table name from the API definition
        String safeTableName = apiDef.getTableName();

        if (conditions.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No conditions provided for update"));
        }

        try {
            ObjectContext context = externalConnectivityService.getContext(apiDef.getConnection());

            // Validate column names against schema
            Set<String> schemaColumns = new HashSet<>(
                    schemaDiscoveryService.getColumns(apiDef.getConnection(), safeTableName));

            StringBuilder sql = new StringBuilder("UPDATE ").append(safeTableName).append(" SET ");
            List<Object> params = new ArrayList<>();

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (!isAllowedIdentifier(entry.getKey(), schemaColumns)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid column: " + entry.getKey()));
                }
                sql.append(entry.getKey()).append(" = ?, ");
                params.add(entry.getValue());
            }

            if (!params.isEmpty()) {
                sql.setLength(sql.length() - 2); // Remove last ", "
            }

            sql.append(" WHERE ");
            for (Map.Entry<String, String> entry : conditions.entrySet()) {
                if (!isAllowedIdentifier(entry.getKey(), schemaColumns)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid column: " + entry.getKey()));
                }
                sql.append(entry.getKey()).append(" = ? AND ");
                params.add(entry.getValue());
            }
            sql.setLength(sql.length() - 5); // Remove last " AND "

            SQLExec.query(sql.toString())
                    .paramsArray(params.toArray())
                    .execute(context);

            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            logger.error("Error updating data in table {}", safeTableName, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Creates new data in a dynamic API endpoint.
     *
     * @param tableName the name of the table to insert into
     * @param data      the data to be inserted
     * @return a success or error response
     */
    @PostMapping("/{tableName}")
    public ResponseEntity<?> createData(@PathVariable String tableName,
            @RequestBody Map<String, Object> data) {
        ApiDefinition apiDef = apiDefinitionService.getApiDefinitionByTableNameAndType(tableName, "REST");
        if (apiDef == null || !apiDef.getAllowedOperations().contains("POST")) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        if (data == null || data.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Request body must contain data to insert"));
        }

        // Use the admin-configured table name from the API definition
        String safeTableName = apiDef.getTableName();

        try {
            ObjectContext context = externalConnectivityService.getContext(apiDef.getConnection());

            // Validate column names against schema
            Set<String> schemaColumns = new HashSet<>(
                    schemaDiscoveryService.getColumns(apiDef.getConnection(), safeTableName));

            StringBuilder sql = new StringBuilder("INSERT INTO ").append(safeTableName).append(" (");
            StringBuilder values = new StringBuilder("VALUES (");
            List<Object> params = new ArrayList<>();

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (!isAllowedIdentifier(entry.getKey(), schemaColumns)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid column: " + entry.getKey()));
                }
                sql.append(entry.getKey()).append(", ");
                values.append("?, ");
                params.add(entry.getValue());
            }

            if (!params.isEmpty()) {
                sql.setLength(sql.length() - 2); // Remove last ", "
                values.setLength(values.length() - 2);
            }
            sql.append(") ").append(values).append(")");

            SQLExec.query(sql.toString())
                    .paramsArray(params.toArray())
                    .execute(context);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "success"));
        } catch (Exception e) {
            logger.error("Error inserting data into table {}", safeTableName, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Deletes data from a dynamic API endpoint based on conditions.
     *
     * @param tableName  the name of the table to delete from
     * @param conditions the WHERE clause conditions
     * @return a success or error response
     */
    @DeleteMapping("/{tableName}")
    public ResponseEntity<?> deleteData(@PathVariable String tableName, @RequestParam Map<String, String> conditions) {
        ApiDefinition apiDef = apiDefinitionService.getApiDefinitionByTableNameAndType(tableName, "REST");
        if (apiDef == null || !apiDef.getAllowedOperations().contains("DELETE")) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        // Use the admin-configured table name from the API definition
        String safeTableName = apiDef.getTableName();

        if (conditions.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No conditions provided for delete"));
        }

        try {
            ObjectContext context = externalConnectivityService.getContext(apiDef.getConnection());

            // Validate column names against schema
            Set<String> schemaColumns = new HashSet<>(
                    schemaDiscoveryService.getColumns(apiDef.getConnection(), safeTableName));

            StringBuilder sql = new StringBuilder("DELETE FROM ").append(safeTableName).append(" WHERE ");
            List<Object> params = new ArrayList<>();

            for (Map.Entry<String, String> entry : conditions.entrySet()) {
                if (!isAllowedIdentifier(entry.getKey(), schemaColumns)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid column: " + entry.getKey()));
                }
                sql.append(entry.getKey()).append(" = ? AND ");
                params.add(entry.getValue());
            }

            sql.setLength(sql.length() - 5); // Remove last " AND "

            SQLExec.query(sql.toString())
                    .paramsArray(params.toArray())
                    .execute(context);

            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            logger.error("Error deleting data from table {}", safeTableName, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Builds a safe column list for SELECT queries from the API definition's
     * included columns, validated against the actual database schema.
     *
     * @param includedColumns comma-separated column list from API definition
     * @param schemaColumns   the set of valid column names from the database
     * @return a validated column list string, or "*" if all columns are allowed
     */
    private String buildColumnList(String includedColumns, Set<String> schemaColumns) {
        if (includedColumns == null || includedColumns.isEmpty()) {
            return "*";
        }

        List<String> validColumns = new ArrayList<>();
        for (String col : includedColumns.split(",")) {
            String trimmed = col.trim();
            if (isAllowedIdentifier(trimmed, schemaColumns)) {
                validColumns.add(trimmed);
            }
        }

        return validColumns.isEmpty() ? "*" : String.join(", ", validColumns);
    }
}
