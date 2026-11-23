package com.db2api.service.api;

import com.db2api.config.DynamicGraphQLProvider;
import com.db2api.persistent.api.ApiDefinition;
import com.db2api.persistent.organization.Client;
import com.db2api.persistent.organization.Organization;
import com.db2api.persistent.organization.OrganizationClient;
import com.db2api.repository.api.ApiDefinitionRepository;
import com.db2api.repository.organization.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiDefinitionServiceTest {

    private ApiDefinitionRepository apiDefinitionRepository;
    private ClientRepository clientRepository;
    private ApiDefinitionService apiDefinitionService;

    @BeforeEach
    void setUp() {
        apiDefinitionRepository = mock(ApiDefinitionRepository.class);
        clientRepository = mock(ClientRepository.class);
        apiDefinitionService = new ApiDefinitionService(
                apiDefinitionRepository,
                clientRepository,
                mock(DynamicGraphQLProvider.class));
    }

    @Test
    void cannotSaveActiveApiWithoutAnActiveUser() {
        ApiDefinition api = new ApiDefinition();

        assertThrows(IllegalStateException.class, () -> apiDefinitionService.saveApiDefinition(api, List.of()));
        verify(apiDefinitionRepository, never()).save(api);
    }

    @Test
    void activeApiCannotBeAssignedToInactiveUser() {
        ApiDefinition api = new ApiDefinition();
        Client user = new Client();
        user.setId(7L);
        user.setActive(false);
        assignToActiveHierarchy(user);
        when(clientRepository.findWithStateById(7L)).thenReturn(Optional.of(user));

        assertThrows(
                IllegalStateException.class,
                () -> apiDefinitionService.saveApiDefinition(api, List.of(user)));
        verify(apiDefinitionRepository, never()).save(api);
    }

    @Test
    void savesApiAssignmentOnCredentialUser() {
        ApiDefinition api = new ApiDefinition();
        api.setId(15L);
        Client user = new Client();
        user.setId(7L);
        assignToActiveHierarchy(user);
        when(clientRepository.findWithStateById(7L)).thenReturn(Optional.of(user));
        when(clientRepository.findByAllowedApis_Id(15L)).thenReturn(List.of());
        when(apiDefinitionRepository.save(api)).thenReturn(api);

        apiDefinitionService.saveApiDefinition(api, List.of(user));

        assertTrue(user.getAllowedApis().contains(api));
        verify(clientRepository).saveAll(any());
    }

    @Test
    void activeApiIsAvailableOnlyToItsActiveAssignedUser() {
        ApiDefinition api = new ApiDefinition();
        api.setId(15L);
        Client user = new Client();
        user.setClientId("client-1");
        assignToActiveHierarchy(user);
        when(apiDefinitionRepository.findById(15L)).thenReturn(Optional.of(api));
        when(clientRepository.findByAllowedApis_Id(15L)).thenReturn(List.of(user));

        assertTrue(apiDefinitionService.isAvailableToClient(15L, "client-1"));
        assertFalse(apiDefinitionService.isAvailableToClient(15L, "client-2"));
    }

    @Test
    void inactiveOrganizationMakesAssignedUserApiUnavailable() {
        ApiDefinition api = new ApiDefinition();
        api.setId(15L);
        Client user = new Client();
        user.setClientId("client-1");
        assignToActiveHierarchy(user);
        user.getOrganizationClient().getOrganization().setStatus("Inactive");
        when(apiDefinitionRepository.findById(15L)).thenReturn(Optional.of(api));
        when(clientRepository.findByAllowedApis_Id(15L)).thenReturn(List.of(user));

        assertFalse(apiDefinitionService.isAvailableToClient(15L, "client-1"));
    }

    private void assignToActiveHierarchy(Client user) {
        Organization organization = new Organization();
        organization.setStatus("Active");
        OrganizationClient namedClient = new OrganizationClient();
        namedClient.setOrganization(organization);
        user.setOrganizationClient(namedClient);
    }
}
