package com.db2api.persistent.admin;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "Username is required")
    @Size(max = 255)
    @Column(name = "username", unique = true)
    private String username;

    /**
     * Encrypted password for the admin user.
     */
    @NotBlank(message = "Password is required")
    @Column(name = "password")
    private String password;

    /**
     * Role assigned to the admin user (e.g., ADMIN, EDITOR, VIEWER).
     */
    @NotBlank(message = "Role is required")
    @Size(max = 50)
    @Column(name = "role")
    private String role;
}
