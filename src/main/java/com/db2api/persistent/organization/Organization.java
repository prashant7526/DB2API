package com.db2api.persistent.organization;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an organization.
 * Organizations own clients and serve as a grouping for API access management.
 */
@Entity
@Table(name = "organization")
@Getter
@Setter
public class Organization {

    /**
     * Primary key ID for the organization.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The name of the organization.
     */
    @NotBlank(message = "Organization name is required")
    @Size(max = 255)
    @Column(name = "name")
    private String name;

    /**
     * The status of the organization (e.g., ACTIVE, INACTIVE).
     */
    @NotBlank(message = "Status is required")
    @Size(max = 50)
    @Column(name = "status")
    private String status;

    public boolean isActive() {
        return status == null || "Active".equalsIgnoreCase(status);
    }

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrganizationClient> clients = new ArrayList<>();

    public void addClient(OrganizationClient client) {
        clients.add(client);
        client.setOrganization(this);
    }

    public void removeClient(OrganizationClient client) {
        clients.remove(client);
        client.setOrganization(null);
    }
}
