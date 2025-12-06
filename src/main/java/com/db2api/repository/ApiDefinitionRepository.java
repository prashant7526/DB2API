package com.db2api.repository;

import com.db2api.persistent.ApiDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiDefinitionRepository extends JpaRepository<ApiDefinition, Long> {

    ApiDefinition findByTableNameAndApiType(String tableName, String apiType);
}
