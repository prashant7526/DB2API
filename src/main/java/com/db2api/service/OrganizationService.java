package com.db2api.service;

import com.db2api.persistent.Client;
import com.db2api.persistent.Organization;
import com.db2api.repository.ClientRepository;
import com.db2api.repository.OrganizationRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final ClientRepository clientRepository;
    private final EncryptionService encryptionService;

    public OrganizationService(OrganizationRepository organizationRepository, ClientRepository clientRepository, EncryptionService encryptionService) {
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

    public void saveClient(Client client, Organization organization) {
        if (client.getOrganization() == null) {
            client.setOrganization(organization);
        }

        // Generate Client ID and Secret if new
        if (client.getClientId() == null) {
            client.setClientId(UUID.randomUUID().toString());
            String rawSecret = UUID.randomUUID().toString();
            client.setClientSecret(encryptionService.encrypt(rawSecret));
            // Note: In a real app, we should show the raw secret to the user ONCE.
            // Here we just store it encrypted.
        }

        clientRepository.save(client);
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
