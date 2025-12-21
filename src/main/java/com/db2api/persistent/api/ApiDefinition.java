package com.db2api.persistent.api;

import com.db2api.persistent.connection.DbConnection;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing an API definition.
 * It maps a database table to a dynamic REST or GraphQL endpoint.
 */
@Entity
@Table(name = "api_definition")
@Getter
@Setter
public class ApiDefinition {

    /**
     * Primary key ID for the API definition.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The name of the database table exposed by this API.
     */
    @Column(name = "table_name")
    private String tableName;

    /**
     * The type of API (e.g., REST, GraphQL).
     */
    @Column(name = "api_type")
    private String apiType;

    /**
     * Comma-separated list of allowed HTTP operations (e.g., GET, PUT, DELETE).
     */
    @Column(name = "allowed_operations")
    private String allowedOperations;

    /**
     * Comma-separated list of columns to be included in the API response.
     */
    @Column(name = "included_columns")
    private String includedColumns;

    /**
     * The database connection used by this API definition.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id")
    private DbConnection connection;
}
