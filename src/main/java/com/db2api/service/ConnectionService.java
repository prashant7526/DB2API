
package com.db2api.service;

import com.db2api.persistent.DbConnection;
import com.db2api.repository.DbConnectionRepository;

import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;


@Service
    private final DbConnectionRepository dbConnectionRepository;
    private final EncryptionService encryptionService;

    public ConnectionService(DbConnectionRepository dbConnectionRepository, EncryptionService encryptionService) {
        this.dbConnectionRepository = dbConnectionRepository;
        this.encryptionService = encryptionService;
    }

    public List<DbConnection> getAllConnections() {
        return dbConnectionRepository.findAll();
    }

    public void saveConnection(DbConnection connection) {
        if (connection.getPassword() != null && !connection.getPassword().isEmpty()) {
             // Ideally use a transient field for the raw password from UI or better logic
             // Encrypt before saving
             connection.setPassword(encryptionService.encrypt(connection.getPassword()));
        }
        dbConnectionRepository.save(connection);
    }

    public void deleteConnection(DbConnection connection) {
        dbConnectionRepository.delete(connection);
    }

    public DbConnection createNewConnection() {
        return new DbConnection();
    }

    public boolean testConnection(DbConnection conn) {
        String decryptedPassword = encryptionService.decrypt(conn.getPassword());
        try (Connection connection = DriverManager.getConnection(conn.getUrl(), conn.getUsername(), decryptedPassword)) {
            return connection.isValid(5);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
