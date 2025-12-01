package com.db2api.service;

import com.db2api.persistent.Client;
import com.db2api.persistent.Organization;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.query.ObjectSelect;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrganizationService {

    private final ServerRuntime serverRuntime;
    private final EncryptionService encryptionService;

    public OrganizationService(ServerRuntime serverRuntime, EncryptionService encryptionService) {
        this.serverRuntime = serverRuntime;
        this.encryptionService = encryptionService;
    }

    private ObjectContext getContext() {
        return serverRuntime.newContext();
    }

    public List<Organization> getAllOrganizations() {
        return ObjectSelect.query(Organization.class).select(getContext());
    }

    public void saveOrganization(Organization organization) {
        ObjectContext context = organization.getObjectContext();
        if (context == null) {
            context = getContext();
            context.registerNewObject(organization);
        }
        context.commitChanges();
    }

    public void deleteOrganization(Organization organization) {
        ObjectContext context = organization.getObjectContext();
        if (context != null) {
            context.deleteObjects(organization);
            context.commitChanges();
        }
    }

    public List<Client> getClients(Organization organization) {
        if (organization.getObjectId().isTemporary()) {
            return List.of();
        }
        // Refreshing to ensure we have the latest list
        return ObjectSelect.query(Client.class)
                .where(Client.ORGANIZATION.eq(organization))
                .select(organization.getObjectContext());
    }

    public void saveClient(Client client, Organization organization) {
        ObjectContext context = client.getObjectContext();
        if (context == null) {
            context = organization.getObjectContext(); // Use same context as organization
            context.registerNewObject(client);
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

        context.commitChanges();
    }

    public void deleteClient(Client client) {
        ObjectContext context = client.getObjectContext();
        if (context != null) {
            context.deleteObjects(client);
            context.commitChanges();
        }
    }

    public Organization createNewOrganization() {
        Organization org = new Organization();
        org.setObjectContext(getContext()); // Attach to a context immediately
        return org;
    }

    public Client createNewClient(Organization org) {
        Client client = new Client();
        client.setObjectContext(org.getObjectContext());
        client.setOrganization(org);
        return client;
    }

    public Client findClientByClientId(String clientId) {
        return ObjectSelect.query(Client.class)
                .where(Client.CLIENT_ID.eq(clientId))
                .selectOne(getContext());
    }
}
