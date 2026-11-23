package com.db2api.service.connection;

import com.db2api.service.EncryptionService;

import com.db2api.persistent.connection.DbConnection;
import com.db2api.repository.connection.DbConnectionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.List;

@Service
public class ConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionService.class);

    private final DbConnectionRepository dbConnectionRepository;
    private final EncryptionService encryptionService;
    private final ExternalConnectivityService externalConnectivityService;

    public ConnectionService(DbConnectionRepository dbConnectionRepository, EncryptionService encryptionService,
            ExternalConnectivityService externalConnectivityService) {
        this.dbConnectionRepository = dbConnectionRepository;
        this.encryptionService = encryptionService;
        this.externalConnectivityService = externalConnectivityService;
    }

    public List<DbConnection> getAllConnections() {
        return dbConnectionRepository.findAll();
    }

    /**
     * Saves a database connection, encrypting the raw password if provided.
     * Encrypts a supplied password, including an empty password for a new
     * passwordless connection. On an existing connection, an empty password
     * preserves the saved encrypted password.
     *
     * @param connection the connection to save
     * @param rawPassword the new plaintext password, or null/empty to keep existing
     */
    public void saveConnection(DbConnection connection, String rawPassword) {
        boolean sqlite = DatabaseType.fromDriverClass(connection.getDriverClass())
                .filter(type -> type == DatabaseType.SQLITE)
                .isPresent();
        if (rawPassword != null && (!rawPassword.isEmpty() || connection.getPassword() == null || sqlite)) {
            connection.setPassword(encryptionService.encrypt(rawPassword));
        }
        dbConnectionRepository.save(connection);
        // Invalidate cached runtime so pool is recreated with updated credentials
        if (connection.getId() != null) {
            externalConnectivityService.invalidateRuntime(connection.getId());
        }
    }

    /**
     * @deprecated Use {@link #saveConnection(DbConnection, String)} instead to
     *             avoid double-encryption bugs.
     */
    @Deprecated
    public void saveConnection(DbConnection connection) {
        if (connection.getPassword() != null && !connection.getPassword().isEmpty()) {
            if (connection.getId() == null) {
                connection.setPassword(encryptionService.encrypt(connection.getPassword()));
            }
        }
        dbConnectionRepository.save(connection);
        if (connection.getId() != null) {
            externalConnectivityService.invalidateRuntime(connection.getId());
        }
    }

    public void deleteConnection(DbConnection connection) {
        dbConnectionRepository.delete(connection);
        // Invalidate cached runtime so pool is shut down
        if (connection.getId() != null) {
            externalConnectivityService.invalidateRuntime(connection.getId());
        }
    }

    public DbConnection createNewConnection() {
        return new DbConnection();
    }

    public boolean testConnection(DbConnection conn) {
        return testConnection(conn, null);
    }

    public boolean testConnection(DbConnection conn, String rawPassword) {
        String decryptedPassword;
        if (rawPassword != null && !rawPassword.isEmpty()) {
            decryptedPassword = rawPassword;
        } else if (conn.getPassword() != null) {
            decryptedPassword = encryptionService.decrypt(conn.getPassword());
        } else {
            decryptedPassword = rawPassword;
        }
        try (Connection connection = JdbcConnectionHelper.open(
                conn.getUrl(), conn.getUsername(), decryptedPassword)) {
            return connection.isValid(5);
        } catch (Exception e) {
            logger.error("Error testing connection", e);
            return false;
        }
    }
}
