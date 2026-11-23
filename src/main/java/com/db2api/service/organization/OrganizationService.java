package com.db2api.service.organization;

import com.db2api.service.EncryptionService;

import com.db2api.persistent.organization.Client;
import com.db2api.persistent.organization.ClientAuthenticationMode;
import com.db2api.persistent.organization.Organization;
import com.db2api.persistent.organization.OrganizationClient;
import com.db2api.persistent.api.ApiDefinition;
import com.db2api.repository.organization.ClientRepository;
import com.db2api.repository.organization.OrganizationClientRepository;
import com.db2api.repository.organization.OrganizationRepository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationClientRepository organizationClientRepository;
    private final ClientRepository clientRepository;
    private final EncryptionService encryptionService;
    private final ClientBearerTokenService bearerTokenService;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            OrganizationClientRepository organizationClientRepository,
            ClientRepository clientRepository,
            EncryptionService encryptionService,
            ClientBearerTokenService bearerTokenService) {
        this.organizationRepository = organizationRepository;
        this.organizationClientRepository = organizationClientRepository;
        this.clientRepository = clientRepository;
        this.encryptionService = encryptionService;
        this.bearerTokenService = bearerTokenService;
    }

    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    public void saveOrganization(Organization organization) {
        if (organization.getId() != null
                && !organization.isActive()
                && getOrganizationClients(organization).stream().anyMatch(OrganizationClient::isActive)) {
            throw new IllegalStateException("Deactivate all active clients before deactivating this organization");
        }
        organizationRepository.save(organization);
    }

    @Transactional
    public void deleteOrganization(Organization organization) {
        List<OrganizationClient> organizationClients = getOrganizationClients(organization);
        if (organizationClients.stream().anyMatch(OrganizationClient::isActive)) {
            throw new IllegalStateException("Deactivate all clients before deleting this organization");
        }

        List<Client> descendantUsers = new ArrayList<>(getClients(organization));
        for (OrganizationClient organizationClient : organizationClients) {
            descendantUsers.addAll(getClientUsers(organizationClient));
        }
        if (descendantUsers.stream().anyMatch(Client::isActive)) {
            throw new IllegalStateException("Deactivate all users before deleting this organization");
        }
        if (descendantUsers.stream()
                .flatMap(user -> user.getAllowedApis().stream())
                .anyMatch(ApiDefinition::isActive)) {
            throw new IllegalStateException("Deactivate all assigned APIs before deleting this organization");
        }
        organizationRepository.delete(organization);
    }

    public List<Client> getClients(Organization organization) {
        if (organization.getId() == null) {
            return List.of();
        }
        return clientRepository.findByOrganization_Id(organization.getId());
    }

    public List<OrganizationClient> getOrganizationClients(Organization organization) {
        if (organization.getId() == null) {
            return List.of();
        }
        return organizationClientRepository.findByOrganization_Id(organization.getId());
    }

    public List<Client> getClientUsers(OrganizationClient organizationClient) {
        if (organizationClient.getId() == null) {
            return List.of();
        }
        return clientRepository.findByOrganizationClient_Id(organizationClient.getId());
    }

    public List<Client> getAllClientUsers() {
        return clientRepository.findAll();
    }

    public OrganizationClient createOrganizationClient(Organization organization) {
        OrganizationClient client = new OrganizationClient();
        client.setOrganization(organization);
        return client;
    }

    public void saveOrganizationClient(OrganizationClient client, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Client name is required");
        }
        if (client.getId() == null && client.isActive()
                && (client.getOrganization() == null || !client.getOrganization().isActive())) {
            throw new IllegalStateException("Activate the organization before adding an active client");
        }
        client.setName(name.trim());
        organizationClientRepository.save(client);
    }

    @Transactional
    public void updateOrganizationClientActive(Long clientId, boolean active) {
        OrganizationClient client = organizationClientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client no longer exists"));
        if (active && (client.getOrganization() == null || !client.getOrganization().isActive())) {
            throw new IllegalStateException("Activate the organization before activating this client");
        }
        if (!active && getClientUsers(client).stream().anyMatch(Client::isActive)) {
            throw new IllegalStateException("Deactivate all active users before deactivating this client");
        }
        client.setActive(active);
        organizationClientRepository.save(client);
    }

    @Transactional
    public void updateClientActive(Long userId, boolean active) {
        Client user = clientRepository.findWithStateById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User no longer exists"));
        if (active && !user.isActiveInHierarchy()) {
            throw new IllegalStateException("Activate the organization and client before activating this user");
        }
        if (!active && user.getAllowedApis().stream().anyMatch(ApiDefinition::isActive)) {
            throw new IllegalStateException("Deactivate assigned APIs before deactivating this user");
        }
        user.setActive(active);
        clientRepository.save(user);
    }

    public void renameClientUser(Client client, String userName) {
        if (userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("User name is required");
        }
        client.setUserName(userName.trim());
        clientRepository.save(client);
    }

    /**
     * Saves a client and generates credentials if new.
     *
     * @param client       the client to save
     * @param organization the organization the client belongs to
     * @return the raw credential for one-time display, or null if this is an existing client
     */
    public String saveClient(Client client, OrganizationClient organizationClient, String userName) {
        if (organizationClient == null || organizationClient.getOrganization() == null) {
            throw new IllegalArgumentException("Select a client organization");
        }
        if (!organizationClient.isActiveInHierarchy()) {
            throw new IllegalStateException("Activate the organization and client before adding an active user");
        }
        if (userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("User name is required");
        }

        client.setUserName(userName.trim());
        client.setOrganizationClient(organizationClient);
        client.setOrganization(organizationClient.getOrganization());
        String rawCredential = null;

        // Generate client credentials when the client is first created.
        if (client.getClientId() == null) {
            client.setClientId(UUID.randomUUID().toString());
            String rawSecret = UUID.randomUUID().toString();
            client.setClientSecret(encryptClientSecret(rawSecret));
            rawCredential = client.getAuthenticationMode() == ClientAuthenticationMode.BEARER
                    ? bearerTokenService.generateToken(client)
                    : rawSecret;
        }

        clientRepository.save(client);
        return rawCredential;
    }

    @Transactional
    public void migrateLegacyClientUsers() {
        List<Client> users = clientRepository.findByOrganizationClientIsNull();
        for (Client user : users) {
            if (user.getUserName() == null || user.getUserName().isBlank()) {
                user.setUserName("API user");
            }
            if (user.getOrganization() != null) {
                OrganizationClient organizationClient = new OrganizationClient();
                organizationClient.setName("Imported client " + user.getId());
                organizationClient.setOrganization(user.getOrganization());
                organizationClient.setActive(user.getOrganization().isActive());
                organizationClientRepository.save(organizationClient);
                user.setOrganizationClient(organizationClient);
                if (user.getActive() == null) {
                    user.setActive(organizationClient.isActive());
                }
            } else if (user.getActive() == null) {
                user.setActive(false);
            }
        }
        clientRepository.saveAll(users);
    }

    public String updateClientAuthenticationMode(Client client, ClientAuthenticationMode authenticationMode) {
        if (authenticationMode == null || authenticationMode == client.getAuthenticationMode()) {
            return null;
        }

        client.setAuthenticationMode(authenticationMode);
        client.setBearerTokenHash(null);

        String rawCredential;
        if (authenticationMode == ClientAuthenticationMode.BEARER) {
            rawCredential = bearerTokenService.generateToken(client);
        } else {
            rawCredential = UUID.randomUUID().toString();
            client.setClientSecret(encryptClientSecret(rawCredential));
        }

        clientRepository.save(client);
        return rawCredential;
    }

    private String encryptClientSecret(String rawSecret) {
        String encryptedSecret = encryptionService.encrypt(rawSecret);
        if (encryptedSecret == null) {
            throw new IllegalStateException("Could not encrypt client secret");
        }
        return encryptedSecret;
    }

    public void deleteClient(Client client) {
        clientRepository.delete(client);
    }

    public Organization createNewOrganization() {
        Organization organization = new Organization();
        organization.setStatus("Active");
        return organization;
    }

    public Client createNewClient(OrganizationClient organizationClient) {
        Client client = new Client();
        client.setOrganizationClient(organizationClient);
        client.setOrganization(organizationClient.getOrganization());
        return client;
    }

    public Client findClientByClientId(String clientId) {
        return clientRepository.findByClientId(clientId).orElse(null);
    }
}
