package com.db2api.repository.admin;

import com.db2api.persistent.admin.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for {@link AdminUser} entities.
 */
@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    /**
     * Finds an admin user by their username.
     * 
     * @param username the username to search for
     * @return an Optional containing the user if found, or empty otherwise
     */
    Optional<AdminUser> findByUsername(String username);
}
