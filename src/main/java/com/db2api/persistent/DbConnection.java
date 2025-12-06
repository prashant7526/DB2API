package com.db2api.persistent;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "db_connection")
@Getter
@Setter
public class DbConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "url")
    private String url;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "driver_class")
    private String driverClass;

    @OneToMany(mappedBy = "connection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApiDefinition> apiDefinitions = new ArrayList<>();

    public void addToApiDefinitions(ApiDefinition apiDefinition) {
        apiDefinitions.add(apiDefinition);
        apiDefinition.setConnection(this);
    }

    public void removeFromApiDefinitions(ApiDefinition apiDefinition) {
        apiDefinitions.remove(apiDefinition);
        apiDefinition.setConnection(null);
    }
}
