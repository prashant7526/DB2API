package com.db2api.service;

import com.db2api.persistent.DbConnection;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Service
public class SchemaDiscoveryService {

    private final EncryptionService encryptionService;

    public SchemaDiscoveryService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public List<String> getTables(DbConnection conn) {
        List<String> tables = new ArrayList<>();
        String decryptedPassword = encryptionService.decrypt(conn.getPassword());
        try (Connection connection = DriverManager.getConnection(conn.getUrl(), conn.getUsername(), decryptedPassword)) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[] { "TABLE", "VIEW" })) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tables;
    }

    public List<String> getColumns(DbConnection conn, String tableName) {
        List<String> columns = new ArrayList<>();
        String decryptedPassword = encryptionService.decrypt(conn.getPassword());
        try (Connection connection = DriverManager.getConnection(conn.getUrl(), conn.getUsername(), decryptedPassword)) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return columns;
    }
}
