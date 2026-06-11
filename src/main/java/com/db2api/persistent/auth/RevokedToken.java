package com.db2api.persistent.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Entity representing a revoked (blacklisted) JWT token.
 * When a user logs out or a token is explicitly revoked, the token's
 * JTI (JWT ID) is stored here until its natural expiration.
 */
@Entity
@Table(name = "revoked_token")
@Getter
@Setter
public class RevokedToken {

    /**
     * The JWT ID (jti claim) of the revoked token.
     */
    @Id
    @Column(name = "jti", length = 64)
    private String jti;

    /**
     * Timestamp when the token was revoked.
     */
    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    /**
     * The expiration time of the token, after which this entry can be cleaned up.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
