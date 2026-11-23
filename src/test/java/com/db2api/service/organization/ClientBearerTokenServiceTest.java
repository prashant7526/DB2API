package com.db2api.service.organization;

import com.db2api.persistent.organization.Client;
import com.db2api.persistent.organization.ClientAuthenticationMode;
import com.db2api.persistent.organization.Organization;
import com.db2api.persistent.organization.OrganizationClient;
import com.db2api.repository.organization.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientBearerTokenServiceTest {

    private ClientRepository clientRepository;
    private ClientBearerTokenService tokenService;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        tokenService = new ClientBearerTokenService(clientRepository);
    }

    @Test
    void generatesOpaqueTokenAndLooksUpItsHash() {
        Client client = new Client();
        client.setClientId("client-1");
        client.setAuthenticationMode(ClientAuthenticationMode.BEARER);
        assignToActiveHierarchy(client);

        String token = tokenService.generateToken(client);
        when(clientRepository.findByBearerTokenHash(client.getBearerTokenHash()))
                .thenReturn(Optional.of(client));

        assertTrue(token.startsWith(ClientBearerTokenService.TOKEN_PREFIX));
        assertNotEquals(token, client.getBearerTokenHash());
        assertSame(client, tokenService.findClientByToken(token).orElseThrow());
    }

    @Test
    void doesNotResolveTokenForInactiveCredentialUser() {
        Client client = new Client();
        client.setClientId("client-1");
        client.setAuthenticationMode(ClientAuthenticationMode.BEARER);
        client.setActive(false);
        assignToActiveHierarchy(client);

        String token = tokenService.generateToken(client);
        when(clientRepository.findByBearerTokenHash(client.getBearerTokenHash()))
                .thenReturn(Optional.of(client));

        assertTrue(tokenService.findClientByToken(token).isEmpty());
    }

    private void assignToActiveHierarchy(Client client) {
        Organization organization = new Organization();
        organization.setStatus("Active");
        OrganizationClient namedClient = new OrganizationClient();
        namedClient.setOrganization(organization);
        client.setOrganizationClient(namedClient);
    }
}
