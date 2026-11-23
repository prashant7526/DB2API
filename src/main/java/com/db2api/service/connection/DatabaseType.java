package com.db2api.service.connection;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public enum DatabaseType {
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", 5432, "jdbc:postgresql://", "Database name", true),
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", 3306, "jdbc:mysql://", "Database name", true),
    MARIADB("MariaDB", "org.mariadb.jdbc.Driver", 3306, "jdbc:mariadb://", "Database name", true),
    SQL_SERVER("Microsoft SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver", 1433,
            "jdbc:sqlserver://", "Database name", true),
    ORACLE("Oracle Database", "oracle.jdbc.OracleDriver", 1521, "jdbc:oracle:thin:@//", "Service name", true),
    SQLITE("SQLite", "org.sqlite.JDBC", null, "jdbc:sqlite:", "Database file path", false),
    DB2("IBM Db2", "com.ibm.db2.jcc.DB2Driver", 50000, "jdbc:db2://", "Database name", true);

    private final String label;
    private final String driverClass;
    private final Integer defaultPort;
    private final String jdbcPrefix;
    private final String databaseFieldLabel;
    private final boolean networkDatabase;

    DatabaseType(
            String label,
            String driverClass,
            Integer defaultPort,
            String jdbcPrefix,
            String databaseFieldLabel,
            boolean networkDatabase) {
        this.label = label;
        this.driverClass = driverClass;
        this.defaultPort = defaultPort;
        this.jdbcPrefix = jdbcPrefix;
        this.databaseFieldLabel = databaseFieldLabel;
        this.networkDatabase = networkDatabase;
    }

    public String getLabel() {
        return label;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public Integer getDefaultPort() {
        return defaultPort;
    }

    public String getDatabaseFieldLabel() {
        return databaseFieldLabel;
    }

    public boolean isNetworkDatabase() {
        return networkDatabase;
    }

    public String buildJdbcUrl(String host, Integer port, String database) {
        String databaseValue = database == null ? "" : database.trim();
        if (databaseValue.isEmpty()) {
            throw new IllegalArgumentException(databaseFieldLabel + " is required");
        }

        if (this == SQLITE) {
            return jdbcPrefix + Path.of(databaseValue).toAbsolutePath().normalize().toString().replace('\\', '/');
        }

        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host name is required");
        }
        if (port == null || port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }

        String address = formatHost(host.trim()) + ":" + port;
        return switch (this) {
            case POSTGRESQL, MYSQL, MARIADB, DB2 -> jdbcPrefix + address + "/" + encodePathSegment(databaseValue);
            case ORACLE -> jdbcPrefix + address + "/" + encodePathSegment(databaseValue);
            case SQL_SERVER -> jdbcPrefix + address + ";databaseName=" + escapeSqlServerProperty(databaseValue)
                    + ";encrypt=true";
            case SQLITE -> throw new IllegalStateException("SQLite URL is handled separately");
        };
    }

    public ConnectionDetails parseJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith(jdbcPrefix)) {
            throw new IllegalArgumentException("JDBC URL does not match " + label);
        }

        if (this == SQLITE) {
            return new ConnectionDetails("", null, jdbcUrl.substring(jdbcPrefix.length()));
        }

        String address;
        String database = "";
        if (this == SQL_SERVER) {
            String remainder = jdbcUrl.substring(jdbcPrefix.length());
            String[] parts = remainder.split(";", -1);
            address = parts[0];
            for (int i = 1; i < parts.length; i++) {
                int separator = parts[i].indexOf('=');
                if (separator > 0 && "databaseName".equalsIgnoreCase(parts[i].substring(0, separator))) {
                    database = unescapeSqlServerProperty(parts[i].substring(separator + 1));
                    break;
                }
            }
        } else {
            address = jdbcUrl.substring(jdbcPrefix.length());
        }

        URI uri = URI.create("//" + address);
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("JDBC URL does not contain a host name");
        }
        Integer port = uri.getPort() < 0 ? defaultPort : uri.getPort();
        if (this != SQL_SERVER) {
            String path = uri.getRawPath();
            if (path != null && path.length() > 1) {
                database = URLDecoder.decode(path.substring(1), StandardCharsets.UTF_8);
            }
        }
        return new ConnectionDetails(host, port, database);
    }

    public static Optional<DatabaseType> fromDriverClass(String driverClass) {
        if (driverClass == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(type -> type.driverClass.equalsIgnoreCase(driverClass.trim()))
                .findFirst();
    }

    private static String formatHost(String host) {
        if (host.contains(":") && !(host.startsWith("[") && host.endsWith("]"))) {
            return "[" + host + "]";
        }
        return host;
    }

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String escapeSqlServerProperty(String value) {
        if (value.matches("[A-Za-z0-9_.$#-]+")) {
            return value;
        }
        return "{" + value.replace("}", "}}") + "}";
    }

    private static String unescapeSqlServerProperty(String value) {
        if (value.startsWith("{") && value.endsWith("}")) {
            return value.substring(1, value.length() - 1).replace("}}", "}");
        }
        return value;
    }

    public record ConnectionDetails(String host, Integer port, String database) {
    }
}
