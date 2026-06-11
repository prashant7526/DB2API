package com.db2api.repository.connection;

import com.db2api.persistent.connection.DbConnection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DbConnectionRepository} covering basic CRUD operations.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DbConnectionRepositoryTest {

    @Autowired
    private DbConnectionRepository dbConnectionRepository;

    @Test
    void createReadUpdateDelete() {
        // Create
        DbConnection connection = new DbConnection();
        connection.setName("Test Connection");
        connection.setUrl("jdbc:h2:mem:testdb");
        connection.setUsername("sa");
        connection.setPassword("password");
        connection.setDriverClass("org.h2.Driver");

        DbConnection saved = dbConnectionRepository.save(connection);
        assertNotNull(saved.getId());

        // Read
        Optional<DbConnection> retrieved = dbConnectionRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("Test Connection", retrieved.get().getName());

        // Update
        retrieved.get().setName("Updated Connection");
        dbConnectionRepository.save(retrieved.get());

        DbConnection updated = dbConnectionRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Updated Connection", updated.getName());

        // Delete
        dbConnectionRepository.delete(updated);
        assertFalse(dbConnectionRepository.existsById(saved.getId()));
    }
}
