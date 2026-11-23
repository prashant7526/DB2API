package com.db2api.service.organization;

import com.db2api.persistent.organization.Client;
import com.db2api.persistent.organization.Organization;
import com.db2api.persistent.organization.OrganizationClient;
import com.db2api.persistent.api.ApiDefinition;
import com.db2api.repository.organization.OrganizationClientRepository;
import com.db2api.repository.organization.ClientRepository;
import com.db2api.repository.organization.OrganizationRepository;
import com.db2api.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganizationServiceTest {

    private ClientRepository clientRepository;
    private OrganizationClientRepository organizationClientRepository;
    private EncryptionService encryptionService;
    private OrganizationRepository organizationRepository;
    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        organizationClientRepository = mock(OrganizationClientRepository.class);
        encryptionService = mock(EncryptionService.class);
        organizationRepository = mock(OrganizationRepository.class);
        organizationService = new OrganizationService(
                organizationRepository,
                organizationClientRepository,
                clientRepository,
                encryptionService,
                mock(ClientBearerTokenService.class));
    }

    @Test
    void getClientsLoadsClientsByOrganizationId() {
        Organization organization = new Organization();
        organization.setId(7L);
        List<Client> clients = List.of(new Client());
        when(clientRepository.findByOrganization_Id(7L)).thenReturn(clients);

        assertSame(clients, organizationService.getClients(organization));
        verify(clientRepository).findByOrganization_Id(7L);
    }

    @Test
    void migrateLegacyClientUsersCreatesNamedClientAndUserName() {
        Organization organization = new Organization();
        organization.setStatus("Active");
        Client legacyUser = new Client();
        legacyUser.setId(12L);
        legacyUser.setOrganization(organization);
        when(clientRepository.findByOrganizationClientIsNull()).thenReturn(List.of(legacyUser));
        when(organizationClientRepository.save(any(OrganizationClient.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        organizationService.migrateLegacyClientUsers();

        assertEquals("API user", legacyUser.getUserName());
        assertEquals("Imported client 12", legacyUser.getOrganizationClient().getName());
        assertSame(organization, legacyUser.getOrganizationClient().getOrganization());
        assertTrue(legacyUser.isActiveInHierarchy());
        verify(clientRepository).saveAll(List.of(legacyUser));
    }

    @Test
    void migratedUserUnderInactiveOrganizationStartsInactive() {
        Organization organization = new Organization();
        organization.setStatus("Inactive");
        Client legacyUser = new Client();
        legacyUser.setId(13L);
        legacyUser.setOrganization(organization);
        legacyUser.setActive(null);
        when(clientRepository.findByOrganizationClientIsNull()).thenReturn(List.of(legacyUser));
        when(organizationClientRepository.save(any(OrganizationClient.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        organizationService.migrateLegacyClientUsers();

        assertFalse(legacyUser.getOrganizationClient().isActive());
        assertFalse(legacyUser.isActive());
        assertFalse(legacyUser.isActiveInHierarchy());
    }

    @Test
    void saveClientAssignsCredentialUserToNamedClient() {
        Organization organization = new Organization();
        OrganizationClient namedClient = new OrganizationClient();
        namedClient.setOrganization(organization);
        Client user = new Client();
        when(encryptionService.encrypt(any(String.class))).thenReturn("encrypted-secret");

        String rawSecret = organizationService.saveClient(user, namedClient, "  Reports API  ");

        assertNotNull(rawSecret);
        assertEquals("Reports API", user.getUserName());
        assertSame(namedClient, user.getOrganizationClient());
        assertSame(organization, user.getOrganization());
        assertEquals("encrypted-secret", user.getClientSecret());
        verify(clientRepository).save(user);
    }

    @Test
    void cannotDeactivateOrganizationWhileAClientIsActive() {
        Organization organization = new Organization();
        organization.setId(5L);
        organization.setStatus("Inactive");
        OrganizationClient client = new OrganizationClient();
        when(organizationClientRepository.findByOrganization_Id(5L)).thenReturn(List.of(client));

        assertThrows(IllegalStateException.class, () -> organizationService.saveOrganization(organization));
        verify(organizationRepository, never()).save(organization);
    }

    @Test
    void cannotDeactivateClientWhileAUserIsActive() {
        Organization organization = new Organization();
        organization.setStatus("Active");
        OrganizationClient client = new OrganizationClient();
        client.setId(8L);
        client.setOrganization(organization);
        when(organizationClientRepository.findById(8L)).thenReturn(Optional.of(client));
        when(clientRepository.findByOrganizationClient_Id(8L)).thenReturn(List.of(new Client()));

        assertThrows(
                IllegalStateException.class,
                () -> organizationService.updateOrganizationClientActive(8L, false));
        verify(organizationClientRepository, never()).save(client);
    }

    @Test
    void cannotActivateClientUnderInactiveOrganization() {
        Organization organization = new Organization();
        organization.setStatus("Inactive");
        OrganizationClient client = new OrganizationClient();
        client.setId(9L);
        client.setOrganization(organization);
        when(organizationClientRepository.findById(9L)).thenReturn(Optional.of(client));

        assertThrows(
                IllegalStateException.class,
                () -> organizationService.updateOrganizationClientActive(9L, true));
        verify(organizationClientRepository, never()).save(client);
    }

    @Test
    void cannotDeactivateUserWhileAnAssignedApiIsActive() {
        Client user = new Client();
        user.setId(11L);
        user.getAllowedApis().add(new ApiDefinition());
        when(clientRepository.findWithStateById(11L)).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class, () -> organizationService.updateClientActive(11L, false));
        verify(clientRepository, never()).save(user);
    }

    @Test
    void cannotDeleteOrganizationWhileAClientIsActive() {
        Organization organization = new Organization();
        organization.setId(14L);
        when(organizationClientRepository.findByOrganization_Id(14L))
                .thenReturn(List.of(new OrganizationClient()));

        assertThrows(IllegalStateException.class, () -> organizationService.deleteOrganization(organization));
        verify(organizationRepository, never()).delete(organization);
    }

    @Test
    void cannotDeleteOrganizationWhileAUserIsActive() {
        Organization organization = new Organization();
        organization.setId(15L);
        OrganizationClient organizationClient = new OrganizationClient();
        organizationClient.setId(16L);
        organizationClient.setActive(false);
        Client user = new Client();
        when(organizationClientRepository.findByOrganization_Id(15L)).thenReturn(List.of(organizationClient));
        when(clientRepository.findByOrganization_Id(15L)).thenReturn(List.of());
        when(clientRepository.findByOrganizationClient_Id(16L)).thenReturn(List.of(user));

        assertThrows(IllegalStateException.class, () -> organizationService.deleteOrganization(organization));
        verify(organizationRepository, never()).delete(organization);
    }

    @Test
    void cannotDeleteOrganizationWhileAnAssignedApiIsActive() {
        Organization organization = new Organization();
        organization.setId(17L);
        OrganizationClient organizationClient = new OrganizationClient();
        organizationClient.setId(18L);
        organizationClient.setActive(false);
        Client user = new Client();
        user.setActive(false);
        user.getAllowedApis().add(new ApiDefinition());
        when(organizationClientRepository.findByOrganization_Id(17L)).thenReturn(List.of(organizationClient));
        when(clientRepository.findByOrganization_Id(17L)).thenReturn(List.of());
        when(clientRepository.findByOrganizationClient_Id(18L)).thenReturn(List.of(user));

        assertThrows(IllegalStateException.class, () -> organizationService.deleteOrganization(organization));
        verify(organizationRepository, never()).delete(organization);
    }

    @Test
    void deletesOrganizationWhenAllDescendantsAreInactive() {
        Organization organization = new Organization();
        organization.setId(19L);
        OrganizationClient organizationClient = new OrganizationClient();
        organizationClient.setId(20L);
        organizationClient.setActive(false);
        Client user = new Client();
        user.setActive(false);
        ApiDefinition api = new ApiDefinition();
        api.setActive(false);
        user.getAllowedApis().add(api);
        when(organizationClientRepository.findByOrganization_Id(19L)).thenReturn(List.of(organizationClient));
        when(clientRepository.findByOrganization_Id(19L)).thenReturn(List.of());
        when(clientRepository.findByOrganizationClient_Id(20L)).thenReturn(List.of(user));

        organizationService.deleteOrganization(organization);

        verify(organizationRepository).delete(organization);
    }
}
