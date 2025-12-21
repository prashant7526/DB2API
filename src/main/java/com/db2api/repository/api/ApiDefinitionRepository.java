package com.db2api.repository.api;

import com.db2api.persistent.api.ApiDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for {@link ApiDefinition} entities.
 */
@Repository
public interface ApiDefinitionRepository extends JpaRepository<ApiDefinition, Long> {

    /**
     * Finds an API definition by table name and API type.
     * 
     * @param tableName the name of the database table
     * @param apiType   the type of API (e.g., REST, GraphQL)
     * @return the ApiDefinition if found, or null otherwise
     */
    ApiDefinition findByTableNameAndApiType(String tableName, String apiType);
}
