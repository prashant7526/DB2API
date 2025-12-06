package com.db2api.repository;

import com.db2api.persistent.DbConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DbConnectionRepository extends JpaRepository<DbConnection, Long> {
}
