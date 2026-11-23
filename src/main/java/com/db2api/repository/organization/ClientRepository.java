package com.db2api.repository.organization;

import com.db2api.persistent.organization.Client;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    @Override
    @EntityGraph(attributePaths = {"organizationClient", "organizationClient.organization"})
    List<Client> findAll();

    @EntityGraph(attributePaths = {"organizationClient", "organizationClient.organization"})
    Optional<Client> findByClientId(String clientId);

    List<Client> findByOrganization_Id(Long organizationId);

    @EntityGraph(attributePaths = {"organizationClient", "organizationClient.organization"})
    List<Client> findByOrganizationClient_Id(Long organizationClientId);

    @EntityGraph(attributePaths = {"organizationClient", "organizationClient.organization", "allowedApis"})
    List<Client> findByAllowedApis_Id(Long apiDefinitionId);

    @EntityGraph(attributePaths = {"organizationClient", "organizationClient.organization", "allowedApis"})
    Optional<Client> findWithStateById(Long id);

    List<Client> findByOrganizationClientIsNull();

    @EntityGraph(attributePaths = {"organizationClient", "organizationClient.organization"})
    Optional<Client> findByBearerTokenHash(String bearerTokenHash);
}
