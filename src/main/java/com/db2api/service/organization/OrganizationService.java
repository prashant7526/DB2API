package com.db2api.service.organization;

import com.db2api.service.EncryptionService;

import com.db2api.persistent.organization.Client;
import com.db2api.persistent.organization.Organization;
import com.db2api.repository.organization.ClientRepository;
import com.db2api.repository.organization.OrganizationRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final ClientRepository clientRepository;
    private final EncryptionService encryptionService;

    public OrganizationService(OrganizationRepository organizationRepository, ClientRepository clientRepository,
            EncryptionService encryptionService) {
        this.organizationRepository = organizationRepository;
        this.clientRepository = clientRepository;
        this.encryptionService = encryptionService;
    }

    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    public void saveOrganization(Organization organization) {
        organizationRepository.save(organization);
    }

    public void deleteOrganization(Organization organization) {
        organizationRepository.delete(organization);
    }

    public List<Client> getClients(Organization organization) {
        if (organization.getId() == null) {
            return List.of();
        }
        return organization.getClients();
    }

    /**
     * Saves a client and generates credentials if new.
     *
     * @param client       the client to save
     * @param organization the organization the client belongs to
     * @return the raw client secret for one-time display, or null if the client already had credentials
     */
    public String saveClient(Client client, Organization organization) {
        String rawSecret = null;

        if (client.getOrganization() == null) {
            client.setOrganization(organization);
        }

        // Generate Client ID and Secret if new
        if (client.getClientId() == null) {
            client.setClientId(UUID.randomUUID().toString());
            rawSecret = UUID.randomUUID().toString();
            client.setClientSecret(encryptionService.encrypt(rawSecret));
        }

        clientRepository.save(client);
        return rawSecret;
    }

    public void deleteClient(Client client) {
        clientRepository.delete(client);
    }

    public Organization createNewOrganization() {
        return new Organization();
    }

    public Client createNewClient(Organization org) {
        Client client = new Client();
        client.setOrganization(org);
        return client;
    }

    public Client findClientByClientId(String clientId) {
        return clientRepository.findByClientId(clientId).orElse(null);
    }
}
