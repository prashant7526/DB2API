package com.db2api.persistent;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "api_definition")
@Getter
@Setter
public class ApiDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "api_type")
    private String apiType;

    @Column(name = "allowed_operations")
    private String allowedOperations;

    @Column(name = "included_columns")
    private String includedColumns;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id")
    private DbConnection connection;
}
