package com.db2api.repository.connection;

import com.db2api.persistent.connection.DbConnection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for {@link DbConnection} entities.
 */
@Repository
public interface DbConnectionRepository extends JpaRepository<DbConnection, Long> {

    @Override
    @EntityGraph(attributePaths = {"organizationClient", "organizationClient.organization"})
    List<DbConnection> findAll();
}
