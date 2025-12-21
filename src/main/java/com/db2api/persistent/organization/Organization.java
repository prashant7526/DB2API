package com.db2api.persistent.organization;

import jakarta.persistence.*;
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
    @Column(name = "name")
    private String name;

    /**
     * The status of the organization (e.g., ACTIVE, INACTIVE).
     */
    @Column(name = "status")
    private String status;

    /**
     * List of clients belonging to this organization.
     */
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Client> clients = new ArrayList<>();

    /**
     * Utility method to add a client to this organization.
     * 
     * @param client the client to add
     */
    public void addToClients(Client client) {
        clients.add(client);
        client.setOrganization(this);
    }

    /**
     * Utility method to remove a client from this organization.
     * 
     * @param client the client to remove
     */
    public void removeFromClients(Client client) {
        clients.remove(client);
        client.setOrganization(null);
    }
}
