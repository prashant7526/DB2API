package com.db2api.controller;

import com.db2api.persistent.ApiDefinition;
import com.db2api.persistent.DbConnection;
import com.db2api.service.ApiDefinitionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/dynamic")
public class DynamicRestController {

    private final ApiDefinitionService apiDefinitionService;
    private final com.db2api.service.EncryptionService encryptionService;

    public DynamicRestController(ApiDefinitionService apiDefinitionService, com.db2api.service.EncryptionService encryptionService) {
        this.apiDefinitionService = apiDefinitionService;
        this.encryptionService = encryptionService;
    }

    @GetMapping("/{tableName}")
    public ResponseEntity<List<Map<String, Object>>> getData(@PathVariable String tableName) {
        // TODO: Validate Client Access (Security)
        
        ApiDefinition apiDef = findApiDefinition(tableName, "REST");
        if (apiDef == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (!apiDef.getAllowedOperations().contains("GET")) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        DbConnection conn = apiDef.getConnection();
        String decryptedPassword = encryptionService.decrypt(conn.getPassword());
        String sql = "SELECT " + (apiDef.getIncludedColumns() != null && !apiDef.getIncludedColumns().isEmpty() ? apiDef.getIncludedColumns() : "*") + " FROM " + tableName;
        
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(conn.getUrl(), conn.getUsername(), decryptedPassword);
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                results.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        
        return ResponseEntity.ok(results);
    }
    
    @PutMapping("/{tableName}")
    public ResponseEntity<?> putData(@PathVariable String tableName, @RequestBody Map<String, Object> data) {
        ApiDefinition apiDef = findApiDefinition(tableName, "REST");
        if (apiDef == null || !apiDef.getAllowedOperations().contains("PUT")) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        DbConnection conn = apiDef.getConnection();
        String decryptedPassword = encryptionService.decrypt(conn.getPassword());
        
        // Construct UPDATE or INSERT query (Upsert is hard without PK knowledge, assuming INSERT for now or simple UPDATE if PK provided)
        // For simplicity in this MVP, let's assume INSERT
        
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        StringBuilder values = new StringBuilder("VALUES (");
        List<Object> params = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            sql.append(entry.getKey()).append(",");
            values.append("?,");
            params.add(entry.getValue());
        }
        
        sql.setLength(sql.length() - 1); // Remove last comma
        values.setLength(values.length() - 1);
        sql.append(") ").append(values).append(")");
        
        try (Connection connection = DriverManager.getConnection(conn.getUrl(), conn.getUsername(), decryptedPassword);
             PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            int rows = stmt.executeUpdate();
            return ResponseEntity.ok(Map.of("rowsAffected", rows));
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{tableName}")
    public ResponseEntity<?> deleteData(@PathVariable String tableName, @RequestParam Map<String, String> conditions) {
        ApiDefinition apiDef = findApiDefinition(tableName, "REST");
        if (apiDef == null || !apiDef.getAllowedOperations().contains("DELETE")) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        DbConnection conn = apiDef.getConnection();
        String decryptedPassword = encryptionService.decrypt(conn.getPassword());
        
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
        
        try (Connection connection = DriverManager.getConnection(conn.getUrl(), conn.getUsername(), decryptedPassword);
             PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            int rows = stmt.executeUpdate();
            return ResponseEntity.ok(Map.of("rowsAffected", rows));
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Helper to find API Definition (In real app, cache this or optimize query)
    private ApiDefinition findApiDefinition(String tableName, String type) {
        return apiDefinitionService.getAllApiDefinitions().stream()
                .filter(api -> api.getTableName().equalsIgnoreCase(tableName) && api.getApiType().equalsIgnoreCase(type))
                .findFirst()
                .orElse(null);
    }
}
