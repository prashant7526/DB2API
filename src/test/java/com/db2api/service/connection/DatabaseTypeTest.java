package com.db2api.service.connection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTypeTest {

    @TempDir
    Path tempDir;

    @Test
    void buildsAndParsesNetworkDatabaseUrls() {
        for (DatabaseType type : DatabaseType.values()) {
            if (!type.isNetworkDatabase()) {
                continue;
            }

            String url = type.buildJdbcUrl("localhost", type.getDefaultPort(), "sales data");

            assertEquals(
                    new DatabaseType.ConnectionDetails("localhost", type.getDefaultPort(), "sales data"),
                    type.parseJdbcUrl(url),
                    type.getLabel());
        }
    }

    @Test
    void buildsAndParsesSqliteFileUrl() {
        String path = Path.of("data", "sample.db").toString();

        String url = DatabaseType.SQLITE.buildJdbcUrl(null, null, path);

        assertEquals(
                new DatabaseType.ConnectionDetails(
                        "",
                        null,
                        Path.of(path).toAbsolutePath().normalize().toString().replace('\\', '/')),
                DatabaseType.SQLITE.parseJdbcUrl(url));
    }

    @Test
    void identifiesDatabaseTypeFromDriverClass() {
        assertEquals(DatabaseType.POSTGRESQL, DatabaseType.fromDriverClass("org.postgresql.Driver").orElseThrow());
        assertEquals(DatabaseType.SQLITE, DatabaseType.fromDriverClass("org.sqlite.JDBC").orElseThrow());
    }

    @Test
    void bundlesEverySupportedJdbcDriver() throws ClassNotFoundException {
        for (DatabaseType type : DatabaseType.values()) {
            Class.forName(type.getDriverClass());
        }
    }

    @Test
    void opensSqliteDatabaseWithoutServerCredentials() throws Exception {
        String path = tempDir.resolve("sample.db").toString();
        String url = DatabaseType.SQLITE.buildJdbcUrl(null, null, path);

        try (Connection connection = JdbcConnectionHelper.open(url, "", "")) {
            assertTrue(connection.isValid(5));
        }
    }
}
