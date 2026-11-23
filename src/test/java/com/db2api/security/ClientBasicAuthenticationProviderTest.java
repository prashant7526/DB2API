package com.db2api.security;

import com.db2api.persistent.organization.Client;
import com.db2api.persistent.organization.ClientAuthenticationMode;
import com.db2api.persistent.organization.Organization;
import com.db2api.persistent.organization.OrganizationClient;
import com.db2api.repository.organization.ClientRepository;
import com.db2api.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientBasicAuthenticationProviderTest {

    private ClientRepository clientRepository;
    private EncryptionService encryptionService;
    private ClientBasicAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        encryptionService = mock(EncryptionService.class);
        provider = new ClientBasicAuthenticationProvider(clientRepository, encryptionService);
    }

    @Test
    void authenticatesClientWhenBasicModeAndSecretMatch() {
        Client client = new Client();
        client.setClientId("client-1");
        client.setClientSecret("encrypted");
        client.setAuthenticationMode(ClientAuthenticationMode.BASIC);
        assignToActiveHierarchy(client);
        when(clientRepository.findByClientId("client-1")).thenReturn(Optional.of(client));
        when(encryptionService.decrypt("encrypted")).thenReturn("client-secret");

        var result = provider.authenticate(
                new UsernamePasswordAuthenticationToken("client-1", "client-secret"));

        assertTrue(result.isAuthenticated());
        assertEquals("client-1", result.getName());
    }

    @Test
    void rejectsClientWhenBasicModeIsNotEnabled() {
        Client client = new Client();
        client.setClientId("client-1");
        client.setAuthenticationMode(ClientAuthenticationMode.OAUTH2);
        when(clientRepository.findByClientId("client-1")).thenReturn(Optional.of(client));

        assertThrows(BadCredentialsException.class, () -> provider.authenticate(
                new UsernamePasswordAuthenticationToken("client-1", "client-secret")));
    }

    @Test
    void rejectsInactiveCredentialUsers() {
        Client client = new Client();
        client.setClientId("client-1");
        client.setClientSecret("encrypted");
        client.setAuthenticationMode(ClientAuthenticationMode.BASIC);
        client.setActive(false);
        when(clientRepository.findByClientId("client-1")).thenReturn(Optional.of(client));

        assertThrows(BadCredentialsException.class, () -> provider.authenticate(
                new UsernamePasswordAuthenticationToken("client-1", "client-secret")));
    }

    private void assignToActiveHierarchy(Client client) {
        Organization organization = new Organization();
        organization.setStatus("Active");
        OrganizationClient namedClient = new OrganizationClient();
        namedClient.setOrganization(organization);
        client.setOrganizationClient(namedClient);
    }
}
