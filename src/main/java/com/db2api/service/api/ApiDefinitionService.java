package com.db2api.service.api;

import com.db2api.config.DynamicGraphQLProvider;
import com.db2api.persistent.api.ApiDefinition;
import com.db2api.persistent.organization.Client;
import com.db2api.repository.api.ApiDefinitionRepository;
import com.db2api.repository.organization.ClientRepository;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ApiDefinitionService {

    private final ApiDefinitionRepository apiDefinitionRepository;
    private final ClientRepository clientRepository;
    private final DynamicGraphQLProvider dynamicGraphQLProvider;

    public ApiDefinitionService(ApiDefinitionRepository apiDefinitionRepository,
            ClientRepository clientRepository,
            @Lazy DynamicGraphQLProvider dynamicGraphQLProvider) {
        this.apiDefinitionRepository = apiDefinitionRepository;
        this.clientRepository = clientRepository;
        this.dynamicGraphQLProvider = dynamicGraphQLProvider;
    }

    public List<ApiDefinition> getAllApiDefinitions() {
        return apiDefinitionRepository.findAll();
    }

    public ApiDefinition getApiDefinitionByTableNameAndType(String tableName, String apiType) {
        return apiDefinitionRepository.findByTableNameAndApiType(tableName, apiType);
    }

    public List<Client> getAuthorizedUsers(ApiDefinition apiDefinition) {
        if (apiDefinition.getId() == null) {
            return List.of();
        }
        return clientRepository.findByAllowedApis_Id(apiDefinition.getId());
    }

    @Transactional
    public void saveApiDefinition(ApiDefinition apiDefinition, List<Client> authorizedUsers) {
        List<Client> selectedUsers = new ArrayList<>();
        for (Client requestedUser : authorizedUsers) {
            if (requestedUser.getId() == null) {
                continue;
            }
            Client user = clientRepository.findWithStateById(requestedUser.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Selected user no longer exists"));
            if (apiDefinition.isActive() && !user.isActiveInHierarchy()) {
                throw new IllegalStateException("Active APIs can only be assigned to active users");
            }
            selectedUsers.add(user);
        }
        if (apiDefinition.isActive() && selectedUsers.isEmpty()) {
            throw new IllegalStateException("Assign at least one active user before activating this API");
        }

        List<Client> previouslyAuthorizedUsers = apiDefinition.getId() == null
                ? List.of()
                : clientRepository.findByAllowedApis_Id(apiDefinition.getId());
        Set<Long> selectedUserIds = new HashSet<>();
        selectedUsers.forEach(user -> selectedUserIds.add(user.getId()));

        ApiDefinition savedApi = apiDefinitionRepository.save(apiDefinition);
        List<Client> changedUsers = new ArrayList<>(previouslyAuthorizedUsers);
        for (Client user : previouslyAuthorizedUsers) {
            if (!selectedUserIds.contains(user.getId())) {
                user.getAllowedApis().removeIf(api -> savedApi.getId().equals(api.getId()));
            }
        }
        for (Client user : selectedUsers) {
            boolean alreadyAssigned = user.getAllowedApis().stream()
                    .anyMatch(api -> savedApi.getId().equals(api.getId()));
            if (!alreadyAssigned) {
                user.getAllowedApis().add(savedApi);
            }
            if (changedUsers.stream().noneMatch(changed -> changed.getId().equals(user.getId()))) {
                changedUsers.add(user);
            }
        }
        clientRepository.saveAll(changedUsers);
        dynamicGraphQLProvider.refreshSchema();
    }

    @Transactional(readOnly = true)
    public boolean isAvailableToClient(Long apiId, String clientId) {
        ApiDefinition api = apiDefinitionRepository.findById(apiId).orElse(null);
        if (api == null || !api.isActive()) {
            return false;
        }
        return clientRepository.findByAllowedApis_Id(apiId).stream()
                .anyMatch(user -> user.getClientId().equals(clientId) && user.isActiveInHierarchy());
    }

    @Transactional
    public void deleteApiDefinition(ApiDefinition apiDefinition) {
        if (apiDefinition.getId() != null) {
            List<Client> authorizedUsers = clientRepository.findByAllowedApis_Id(apiDefinition.getId());
            for (Client user : authorizedUsers) {
                user.getAllowedApis().removeIf(api -> apiDefinition.getId().equals(api.getId()));
            }
            clientRepository.saveAll(authorizedUsers);
        }
        apiDefinitionRepository.delete(apiDefinition);
        dynamicGraphQLProvider.refreshSchema();
    }

    public ApiDefinition createNewApiDefinition() {
        return new ApiDefinition();
    }
}
