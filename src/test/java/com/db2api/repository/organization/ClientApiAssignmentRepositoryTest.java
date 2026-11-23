package com.db2api.repository.organization;

import com.db2api.persistent.api.ApiDefinition;
import com.db2api.persistent.organization.Client;
import com.db2api.persistent.organization.Organization;
import com.db2api.persistent.organization.OrganizationClient;
import com.db2api.repository.api.ApiDefinitionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClientApiAssignmentRepositoryTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationClientRepository organizationClientRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ApiDefinitionRepository apiDefinitionRepository;

    @Test
    void loadsActiveApiAssignmentsAndClientHierarchy() {
        Organization organization = new Organization();
        organization.setName("Test organization");
        organization.setStatus("Active");
        organizationRepository.saveAndFlush(organization);

        OrganizationClient namedClient = new OrganizationClient();
        namedClient.setName("Test client");
        namedClient.setOrganization(organization);
        organizationClientRepository.saveAndFlush(namedClient);

        Client user = new Client();
        user.setUserName("Test user");
        user.setClientId("test-client-id");
        user.setClientSecret("encrypted-secret");
        user.setOrganization(organization);
        user.setOrganizationClient(namedClient);
        clientRepository.saveAndFlush(user);

        ApiDefinition api = new ApiDefinition();
        api.setName("Test API");
        api.setTableName("test_table");
        api.setApiType("REST");
        api.setAllowedOperations("GET");
        apiDefinitionRepository.saveAndFlush(api);

        user.getAllowedApis().add(api);
        clientRepository.saveAndFlush(user);

        Client loadedUser = clientRepository.findWithStateById(user.getId()).orElseThrow();
        assertTrue(loadedUser.isActiveInHierarchy());
        assertEquals(1, loadedUser.getAllowedApis().size());

        Client authenticatedUser = clientRepository.findByClientId(user.getClientId()).orElseThrow();
        assertTrue(authenticatedUser.isActiveInHierarchy());

        assertEquals(1, clientRepository.findByAllowedApis_Id(api.getId()).size());
    }
}
