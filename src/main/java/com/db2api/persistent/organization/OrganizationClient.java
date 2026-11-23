package com.db2api.persistent.organization;

import com.db2api.persistent.connection.DbConnection;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A named client entity, such as a subsidiary, that belongs to an organization.
 */
@Entity
@Table(name = "organization_client")
@Getter
@Setter
public class OrganizationClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Client name is required")
    @Size(max = 255)
    @Column(name = "name")
    private String name;

    @Column(name = "active")
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @OneToMany(mappedBy = "organizationClient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Client> users = new ArrayList<>();

    @OneToMany(mappedBy = "organizationClient")
    private List<DbConnection> connections = new ArrayList<>();

    public boolean isActive() {
        return active == null || active;
    }

    public boolean isActiveInHierarchy() {
        return isActive() && organization != null && organization.isActive();
    }

    public void addUser(Client user) {
        users.add(user);
        user.setOrganizationClient(this);
        user.setOrganization(organization);
    }

    public void removeUser(Client user) {
        users.remove(user);
        user.setOrganizationClient(null);
        user.setOrganization(null);
    }
}
