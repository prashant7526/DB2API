package com.db2api.repository.auth;

import com.db2api.persistent.auth.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Repository interface for {@link RevokedToken} entities.
 */
@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

    /**
     * Checks whether a token with the given JTI has been revoked.
     *
     * @param jti the JWT ID to check
     * @return true if the token is revoked
     */
    boolean existsByJti(String jti);

    /**
     * Deletes all revoked token entries that have expired.
     * Useful for periodic cleanup.
     *
     * @param now the current time
     * @return the number of entries deleted
     */
    long deleteByExpiresAtBefore(Instant now);
}
