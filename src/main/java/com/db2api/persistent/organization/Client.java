package com.db2api.persistent.organization;

import com.db2api.persistent.api.ApiDefinition;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a client application that consumes the dynamic APIs.
 * Stores OAuth2 credentials (clientId, clientSecret) for API authentication.
 */
@Entity
@Table(name = "client")
@Getter
@Setter
public class Client {

    /**
     * Primary key ID for the client.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique client identifier used for OAuth2.
     */
    @NotBlank(message = "Client ID is required")
    @Column(name = "client_id", unique = true)
    private String clientId;

    /**
     * Encrypted client secret used for OAuth2.
     */
    @NotBlank(message = "Client secret is required")
    @Column(name = "client_secret")
    private String clientSecret;

    /**
     * The organization to which this client belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    /**
     * The list of API definitions this client is authorized to access.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "client_api_access",
            joinColumns = @JoinColumn(name = "client_id"),
            inverseJoinColumns = @JoinColumn(name = "api_definition_id")
    )
    private List<ApiDefinition> allowedApis = new ArrayList<>();
}
