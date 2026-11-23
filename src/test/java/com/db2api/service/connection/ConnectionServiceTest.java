package com.db2api.service.connection;

import com.db2api.persistent.connection.DbConnection;
import com.db2api.repository.connection.DbConnectionRepository;
import com.db2api.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectionServiceTest {

    private DbConnectionRepository repository;
    private EncryptionService encryptionService;
    private ExternalConnectivityService externalConnectivityService;
    private ConnectionService connectionService;

    @BeforeEach
    void setUp() {
        repository = mock(DbConnectionRepository.class);
        encryptionService = mock(EncryptionService.class);
        externalConnectivityService = mock(ExternalConnectivityService.class);
        connectionService = new ConnectionService(repository, encryptionService, externalConnectivityService);
    }

    @Test
    void encryptsAnEmptyPasswordForNewPasswordlessConnection() {
        DbConnection connection = new DbConnection();
        connection.setDriverClass(DatabaseType.SQLITE.getDriverClass());
        when(encryptionService.encrypt("")).thenReturn("encrypted-empty-password");

        connectionService.saveConnection(connection, "");

        assertEquals("encrypted-empty-password", connection.getPassword());
        verify(repository).save(connection);
    }

    @Test
    void preservesSavedPasswordWhenPasswordFieldIsLeftBlank() {
        DbConnection connection = new DbConnection();
        connection.setId(42L);
        connection.setDriverClass(DatabaseType.POSTGRESQL.getDriverClass());
        connection.setPassword("existing-encrypted-password");

        connectionService.saveConnection(connection, "");

        assertEquals("existing-encrypted-password", connection.getPassword());
        verify(repository).save(connection);
    }
}
