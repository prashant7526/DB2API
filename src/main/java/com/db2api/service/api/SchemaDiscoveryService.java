package com.db2api.service.api;

import com.db2api.service.EncryptionService;

import com.db2api.persistent.connection.DbConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for discovering database schema information such as tables, columns,
 * and their SQL types.
 */
@Service
public class SchemaDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(SchemaDiscoveryService.class);

    private final EncryptionService encryptionService;

    public SchemaDiscoveryService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public List<String> getTables(DbConnection conn) {
        List<String> tables = new ArrayList<>();
        String decryptedPassword = encryptionService.decrypt(conn.getPassword());
        try (Connection connection = DriverManager.getConnection(conn.getUrl(), conn.getUsername(),
                decryptedPassword)) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[] { "TABLE", "VIEW" })) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        } catch (Exception e) {
            logger.error("Error discovering tables", e);
        }
        return tables;
    }

    public List<String> getColumns(DbConnection conn, String tableName) {
        List<String> columns = new ArrayList<>();
        String decryptedPassword = encryptionService.decrypt(conn.getPassword());
        try (Connection connection = DriverManager.getConnection(conn.getUrl(), conn.getUsername(),
                decryptedPassword)) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME"));
                }
            }
        } catch (Exception e) {
            logger.error("Error discovering columns", e);
        }
        return columns;
    }

    /**
     * Returns a map of column names to their SQL type codes for the given table.
     *
     * @param conn      the database connection to inspect
     * @param tableName the table to inspect
     * @return an ordered map of column name to java.sql.Types constant
     */
    public Map<String, Integer> getColumnTypes(DbConnection conn, String tableName) {
        Map<String, Integer> columns = new LinkedHashMap<>();
        String decryptedPassword = encryptionService.decrypt(conn.getPassword());
        try (Connection connection = DriverManager.getConnection(conn.getUrl(), conn.getUsername(),
                decryptedPassword)) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
                while (rs.next()) {
                    columns.put(rs.getString("COLUMN_NAME"), rs.getInt("DATA_TYPE"));
                }
            }
        } catch (Exception e) {
            logger.error("Error discovering column types", e);
        }
        return columns;
    }

    /**
     * Maps a java.sql.Types constant to a GraphQL type string.
     *
     * @param sqlType the SQL type code from java.sql.Types
     * @return the corresponding GraphQL type name
     */
    public static String mapSqlTypeToGraphQL(int sqlType) {
        return switch (sqlType) {
            case Types.SMALLINT, Types.TINYINT, Types.INTEGER -> "Int";
            case Types.BIGINT -> "Int";
            case Types.FLOAT, Types.REAL -> "Float";
            case Types.DOUBLE, Types.NUMERIC, Types.DECIMAL -> "Float";
            case Types.BIT, Types.BOOLEAN -> "Boolean";
            case Types.DATE -> "String"; // Could use custom Date scalar
            case Types.TIME, Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> "String";
            default -> "String";
        };
    }
}
