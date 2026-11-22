package com.db2api.service.organization;

import com.db2api.persistent.organization.Client;
import com.db2api.persistent.organization.Organization;
import com.db2api.repository.organization.ClientRepository;
import com.db2api.repository.organization.OrganizationRepository;
import com.db2api.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganizationServiceTest {

    private ClientRepository clientRepository;
    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        organizationService = new OrganizationService(
                mock(OrganizationRepository.class),
                clientRepository,
                mock(EncryptionService.class));
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
}
