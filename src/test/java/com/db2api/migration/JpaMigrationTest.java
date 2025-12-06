package com.db2api.migration;

import com.db2api.persistent.DbConnection;
import com.db2api.repository.DbConnectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class JpaMigrationTest {

    @Autowired
    private DbConnectionRepository dbConnectionRepository;

    @Test
    public void testDbConnectionRepository() {
        // Create a new DbConnection
        DbConnection connection = new DbConnection();
        connection.setName("Test Connection");
        connection.setUrl("jdbc:h2:mem:testdb");
        connection.setUsername("sa");
        connection.setPassword("password");
        connection.setDriverClass("org.h2.Driver");

        // Save it
        DbConnection savedConnection = dbConnectionRepository.save(connection);
        assertNotNull(savedConnection.getId());

        // Retrieve it
        Optional<DbConnection> retrievedConnection = dbConnectionRepository.findById(savedConnection.getId());
        assertTrue(retrievedConnection.isPresent());
        assertEquals("Test Connection", retrievedConnection.get().getName());

        // Update it
        retrievedConnection.get().setName("Updated Connection");
        dbConnectionRepository.save(retrievedConnection.get());

        // Verify update
        DbConnection updatedConnection = dbConnectionRepository.findById(savedConnection.getId()).orElseThrow();
        assertEquals("Updated Connection", updatedConnection.getName());

        // Delete it
        dbConnectionRepository.delete(updatedConnection);
        assertFalse(dbConnectionRepository.existsById(savedConnection.getId()));
    }
}
