package com.db2api.persistent.organization;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
    @Column(name = "client_id", unique = true)
    private String clientId;

    /**
     * Encrypted client secret used for OAuth2.
     */
    @Column(name = "client_secret")
    private String clientSecret;

    /**
     * The organization to which this client belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;
}
