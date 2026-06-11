package com.db2api.service.connection;

import com.db2api.service.EncryptionService;

import com.db2api.persistent.connection.DbConnection;
import com.db2api.repository.connection.DbConnectionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
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
     * Only encrypts when a new raw password is supplied via the transient
     * {@code rawPassword} field. If the connection already has an encrypted
     * password and no new raw password is given, the existing encrypted
     * password is preserved.
     *
     * @param connection the connection to save
     * @param rawPassword the new plaintext password, or null/empty to keep existing
     */
    public void saveConnection(DbConnection connection, String rawPassword) {
        if (rawPassword != null && !rawPassword.isEmpty()) {
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
        String decryptedPassword = encryptionService.decrypt(conn.getPassword());
        try (Connection connection = DriverManager.getConnection(conn.getUrl(), conn.getUsername(),
                decryptedPassword)) {
            return connection.isValid(5);
        } catch (Exception e) {
            logger.error("Error testing connection", e);
            return false;
        }
    }
}
