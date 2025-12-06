package com.db2api.controller;

import com.db2api.persistent.ApiDefinition;
import com.db2api.service.ApiDefinitionService;
import com.db2api.service.ExternalConnectivityService;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.SQLExec;
import org.apache.cayenne.query.SQLSelect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dynamic")
public class DynamicRestController {

    private final ApiDefinitionService apiDefinitionService;
    private final ExternalConnectivityService externalConnectivityService;

    public DynamicRestController(ApiDefinitionService apiDefinitionService, ExternalConnectivityService externalConnectivityService) {
        this.apiDefinitionService = apiDefinitionService;
        this.externalConnectivityService = externalConnectivityService;
    }

    @GetMapping("/{tableName}")
    public ResponseEntity<List<Map<String, Object>>> getData(@PathVariable String tableName) {
        // TODO: Validate Client Access (Security)

        ApiDefinition apiDef = apiDefinitionService.getApiDefinitionByTableNameAndType(tableName, "REST");
        if (apiDef == null) {
            return ResponseEntity.notFound().build();
        }

        if (!apiDef.getAllowedOperations().contains("GET")) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        try {
            ObjectContext context = externalConnectivityService.getContext(apiDef.getConnection());

            String columns = (apiDef.getIncludedColumns() != null && !apiDef.getIncludedColumns().isEmpty()) ? apiDef.getIncludedColumns() : "*";
            String sql = "SELECT " + columns + " FROM " + tableName;

            List<DataRow> rows = SQLSelect.dataRowQuery(sql).select(context);

            // Convert DataRows to List<Map<String, Object>>
            List<Map<String, Object>> results = new ArrayList<>();
            for (DataRow row : rows) {
                results.add(new HashMap<>(row));
            }

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            // In production, log this error
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{tableName}")
    public ResponseEntity<?> putData(@PathVariable String tableName, @RequestBody Map<String, Object> data) {
        ApiDefinition apiDef = apiDefinitionService.getApiDefinitionByTableNameAndType(tableName, "REST");
        if (apiDef == null || !apiDef.getAllowedOperations().contains("PUT")) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        try {
            ObjectContext context = externalConnectivityService.getContext(apiDef.getConnection());

            // Construct UPDATE or INSERT query (simplified)
            StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
            StringBuilder values = new StringBuilder("VALUES (");
            List<Object> params = new ArrayList<>();

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                sql.append(entry.getKey()).append(",");
                values.append("?,");
                params.add(entry.getValue());
            }

            if (!params.isEmpty()) {
                sql.setLength(sql.length() - 1); // Remove last comma
                values.setLength(values.length() - 1);
            }
            sql.append(") ").append(values).append(")");

            SQLExec.query(sql.toString())
                    .paramsArray(params.toArray())
                    .execute(context);

            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{tableName}")
    public ResponseEntity<?> deleteData(@PathVariable String tableName, @RequestParam Map<String, String> conditions) {
        ApiDefinition apiDef = apiDefinitionService.getApiDefinitionByTableNameAndType(tableName, "REST");
        if (apiDef == null || !apiDef.getAllowedOperations().contains("DELETE")) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        try {
            ObjectContext context = externalConnectivityService.getContext(apiDef.getConnection());

            StringBuilder sql = new StringBuilder("DELETE FROM ").append(tableName).append(" WHERE ");
            List<Object> params = new ArrayList<>();

            if (conditions.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No conditions provided for delete"));
            }

            for (Map.Entry<String, String> entry : conditions.entrySet()) {
                sql.append(entry.getKey()).append(" = ? AND ");
                params.add(entry.getValue());
            }

            sql.setLength(sql.length() - 5); // Remove last " AND "

            SQLExec.query(sql.toString())
                    .paramsArray(params.toArray())
                    .execute(context);

            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
