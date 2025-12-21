package com.db2api.persistent.connection;

import com.db2api.persistent.api.ApiDefinition;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a database connection configuration.
 * Stores metadata and credentials required to connect to external databases.
 */
@Entity
@Table(name = "db_connection")
@Getter
@Setter
public class DbConnection {

    /**
     * Primary key ID for the database connection.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable name for the connection.
     */
    @Column(name = "name")
    private String name;

    /**
     * JDBC URL for the database connection.
     */
    @Column(name = "url")
    private String url;

    /**
     * Username for the database connection.
     */
    @Column(name = "username")
    private String username;

    /**
     * Encrypted password for the database connection.
     */
    @Column(name = "password")
    private String password;

    /**
     * Fully qualified name of the JDBC driver class.
     */
    @Column(name = "driver_class")
    private String driverClass;

    /**
     * List of API definitions associated with this database connection.
     */
    @OneToMany(mappedBy = "connection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApiDefinition> apiDefinitions = new ArrayList<>();

    /**
     * Utility method to add an API definition to this connection.
     * 
     * @param apiDefinition the API definition to add
     */
    public void addToApiDefinitions(ApiDefinition apiDefinition) {
        apiDefinitions.add(apiDefinition);
        apiDefinition.setConnection(this);
    }

    /**
     * Utility method to remove an API definition from this connection.
     * 
     * @param apiDefinition the API definition to remove
     */
    public void removeFromApiDefinitions(ApiDefinition apiDefinition) {
        apiDefinitions.remove(apiDefinition);
        apiDefinition.setConnection(null);
    }
}
