package com.db2api.service.api;

import com.db2api.config.DynamicGraphQLProvider;
import com.db2api.persistent.api.ApiDefinition;
import com.db2api.repository.api.ApiDefinitionRepository;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiDefinitionService {

    private final ApiDefinitionRepository apiDefinitionRepository;
    private final DynamicGraphQLProvider dynamicGraphQLProvider;

    public ApiDefinitionService(ApiDefinitionRepository apiDefinitionRepository,
            @Lazy DynamicGraphQLProvider dynamicGraphQLProvider) {
        this.apiDefinitionRepository = apiDefinitionRepository;
        this.dynamicGraphQLProvider = dynamicGraphQLProvider;
    }

    public List<ApiDefinition> getAllApiDefinitions() {
        return apiDefinitionRepository.findAll();
    }

    public ApiDefinition getApiDefinitionByTableNameAndType(String tableName, String apiType) {
        return apiDefinitionRepository.findByTableNameAndApiType(tableName, apiType);
    }

    public void saveApiDefinition(ApiDefinition apiDefinition) {
        apiDefinitionRepository.save(apiDefinition);
        dynamicGraphQLProvider.refreshSchema();
    }

    public void deleteApiDefinition(ApiDefinition apiDefinition) {
        apiDefinitionRepository.delete(apiDefinition);
        dynamicGraphQLProvider.refreshSchema();
    }

    public ApiDefinition createNewApiDefinition() {
        return new ApiDefinition();
    }
}
