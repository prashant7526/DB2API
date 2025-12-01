package com.db2api.config;

import com.db2api.persistent.ApiDefinition;
import com.db2api.persistent.DbConnection;
import com.db2api.service.ApiDefinitionService;
import com.db2api.service.EncryptionService;
import com.db2api.service.SchemaDiscoveryService;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DynamicGraphQLProvider {

    private final ApiDefinitionService apiDefinitionService;
    private final SchemaDiscoveryService schemaDiscoveryService;
    private final EncryptionService encryptionService;
    
    private GraphQL graphQL;

    public DynamicGraphQLProvider(ApiDefinitionService apiDefinitionService,
                                  SchemaDiscoveryService schemaDiscoveryService,
                                  EncryptionService encryptionService) {
        this.apiDefinitionService = apiDefinitionService;
        this.schemaDiscoveryService = schemaDiscoveryService;
        this.encryptionService = encryptionService;
    }

    @PostConstruct
    public void init() {
        refreshSchema();
    }

    @Bean
    public GraphQL graphQL() {
        return graphQL;
    }

    public void refreshSchema() {
        StringBuilder sdl = new StringBuilder();
        sdl.append("type Query {\n");
        
        List<ApiDefinition> apis = apiDefinitionService.getAllApiDefinitions();
        Map<String, DataFetcher> dataFetchers = new HashMap<>();

        for (ApiDefinition api : apis) {
            if ("GraphQL".equalsIgnoreCase(api.getApiType())) {
                String tableName = api.getTableName();
                String typeName = capitalize(tableName);
                
                // Add query field
                sdl.append("  ").append(tableName).append(": [").append(typeName).append("]\n");
                
                // Define data fetcher
                dataFetchers.put(tableName, env -> fetchData(api));
            }
        }
        sdl.append("}\n\n");

        // Generate Types
        for (ApiDefinition api : apis) {
            if ("GraphQL".equalsIgnoreCase(api.getApiType())) {
                String tableName = api.getTableName();
                String typeName = capitalize(tableName);
                sdl.append("type ").append(typeName).append(" {\n");
                
                // For simplicity, we assume all columns are Strings for now or we inspect DB
                // In a real app, we would map SQL types to GraphQL types
                List<String> columns = schemaDiscoveryService.getColumns(api.getConnection(), tableName);
                for (String col : columns) {
                    sdl.append("  ").append(col).append(": String\n");
                }
                sdl.append("}\n");
            }
        }

        if (dataFetchers.isEmpty()) {
            // Minimal schema to avoid errors if no APIs defined
            sdl = new StringBuilder("type Query { hello: String }");
            dataFetchers.put("hello", env -> "World");
        }

        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl.toString());
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", builder -> {
                    dataFetchers.forEach(builder::dataFetcher);
                    return builder;
                })
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
        this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    private List<Map<String, Object>> fetchData(ApiDefinition api) {
        DbConnection conn = api.getConnection();
        String decryptedPassword = encryptionService.decrypt(conn.getPassword());
        String sql = "SELECT * FROM " + api.getTableName(); // Respect included columns in real impl
        
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(conn.getUrl(), conn.getUsername(), decryptedPassword);
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                results.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
