package com.db2api.repository.connection;

import com.db2api.persistent.connection.DbConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for {@link DbConnection} entities.
 */
@Repository
public interface DbConnectionRepository extends JpaRepository<DbConnection, Long> {
}
