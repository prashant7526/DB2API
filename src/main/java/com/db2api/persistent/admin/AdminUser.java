package com.db2api.persistent.admin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing an administrator user of the system.
 * Used for authentication and role-based access control within the management
 * UI.
 */
@Entity
@Table(name = "admin_user")
@Getter
@Setter
public class AdminUser {

    /**
     * Primary key ID for the admin user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique username for the admin user.
     */
    @Column(name = "username", unique = true)
    private String username;

    /**
     * Encrypted password for the admin user.
     */
    @Column(name = "password")
    private String password;

    /**
     * Role assigned to the admin user (e.g., ADMIN, EDITOR, VIEWER).
     */
    @Column(name = "role")
    private String role;
}
