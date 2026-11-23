package com.db2api.service.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class JdbcConnectionHelper {

    private JdbcConnectionHelper() {
    }

    public static Connection open(String url, String username, String password) throws SQLException {
        if ((username == null || username.isBlank()) && (password == null || password.isBlank())) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, username, password == null ? "" : password);
    }
}
