package com.db2api.repository.organization;

import com.db2api.persistent.organization.OrganizationClient;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationClientRepository extends JpaRepository<OrganizationClient, Long> {

    @EntityGraph(attributePaths = "organization")
    List<OrganizationClient> findByOrganization_Id(Long organizationId);
}
