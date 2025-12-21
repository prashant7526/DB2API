package com.db2api.service.api;

import com.db2api.persistent.api.ApiDefinition;
import com.db2api.repository.api.ApiDefinitionRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiDefinitionService {

    private final ApiDefinitionRepository apiDefinitionRepository;

    public ApiDefinitionService(ApiDefinitionRepository apiDefinitionRepository) {
        this.apiDefinitionRepository = apiDefinitionRepository;
    }

    public List<ApiDefinition> getAllApiDefinitions() {
        return apiDefinitionRepository.findAll();
    }

    public ApiDefinition getApiDefinitionByTableNameAndType(String tableName, String apiType) {
        return apiDefinitionRepository.findByTableNameAndApiType(tableName, apiType);
    }

    public void saveApiDefinition(ApiDefinition apiDefinition) {
        apiDefinitionRepository.save(apiDefinition);
    }

    public void deleteApiDefinition(ApiDefinition apiDefinition) {
        apiDefinitionRepository.delete(apiDefinition);
    }

    public ApiDefinition createNewApiDefinition() {
        return new ApiDefinition();
    }
}
