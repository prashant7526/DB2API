package com.db2api.config;

import com.db2api.persistent.api.ApiDefinition;
import com.db2api.service.api.ApiDefinitionService;
import com.db2api.service.api.SchemaDiscoveryService;
import com.db2api.service.connection.ExternalConnectivityService;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.SQLSelect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Component that provides a dynamic GraphQL schema based on configured API
 * definitions.
 * It rebuilds the schema and data fetchers at runtime to expose database tables
 * as GraphQL fields. Call {@link #refreshSchema()} after API definition changes
 * to update the live schema.
 */
@Component
public class DynamicGraphQLProvider {

    private static final Logger logger = LoggerFactory.getLogger(DynamicGraphQLProvider.class);

    private final ApiDefinitionService apiDefinitionService;
    private final SchemaDiscoveryService schemaDiscoveryService;
    private final ExternalConnectivityService externalConnectivityService;

    private volatile GraphQL graphQL;

    /**
     * Constructs the DynamicGraphQLProvider with required services.
     *
     * @param apiDefinitionService        the service for API definitions
     * @param schemaDiscoveryService      the service for discovering database schemas
     * @param externalConnectivityService the service for external database connectivity
     */
    public DynamicGraphQLProvider(ApiDefinitionService apiDefinitionService,
            SchemaDiscoveryService schemaDiscoveryService,
            ExternalConnectivityService externalConnectivityService) {
        this.apiDefinitionService = apiDefinitionService;
        this.schemaDiscoveryService = schemaDiscoveryService;
        this.externalConnectivityService = externalConnectivityService;
    }

    /**
     * Initializes the GraphQL schema after the component is constructed.
     */
    @PostConstruct
    public void init() {
        refreshSchema();
    }

    /**
     * Provides the GraphQL instance as a Spring Bean.
     * Returns the current volatile reference so refreshes are visible immediately.
     *
     * @return the current GraphQL instance
     */
    @Bean
    public GraphQL graphQL() {
        return graphQL;
    }

    /**
     * Regenerates the GraphQL schema based on current API definitions.
     * Should be called after any API definition is created, updated, or deleted.
     */
    public synchronized void refreshSchema() {
        StringBuilder sdl = new StringBuilder();
        sdl.append("type Query {\n");

        List<ApiDefinition> apis = apiDefinitionService.getAllApiDefinitions();
        Map<String, DataFetcher<?>> queryFetchers = new HashMap<>();
        Map<String, DataFetcher<?>> mutationFetchers = new HashMap<>();
        StringBuilder mutationSdl = new StringBuilder("type Mutation {\n");

        for (ApiDefinition api : apis) {
            if ("GraphQL".equalsIgnoreCase(api.getApiType())) {
                String tableName = api.getTableName();
                String typeName = capitalize(tableName);
                String allowedOps = api.getAllowedOperations() != null ? api.getAllowedOperations() : "";

                // Query field (GET)
                if (allowedOps.contains("GET")) {
                    sdl.append("  ").append(tableName).append(": [").append(typeName).append("]\n");
                    queryFetchers.put(tableName, env -> fetchData(api));
                }

                // Mutation: insert (POST)
                if (allowedOps.contains("POST")) {
                    String inputTypeName = typeName + "Input";
                    mutationSdl.append("  insert").append(typeName).append("(data: ").append(inputTypeName).append("!): String\n");
                    mutationFetchers.put("insert" + typeName, env -> insertData(api, env.getArgument("data")));
                }

                // Mutation: update (PUT)
                if (allowedOps.contains("PUT")) {
                    String inputTypeName = typeName + "Input";
                    mutationSdl.append("  update").append(typeName).append("(data: ").append(inputTypeName).append("!, conditions: ").append(inputTypeName).append("!): String\n");
                    mutationFetchers.put("update" + typeName,
                            env -> updateData(api, env.getArgument("data"), env.getArgument("conditions")));
                }

                // Mutation: delete (DELETE)
                if (allowedOps.contains("DELETE")) {
                    String inputTypeName = typeName + "Input";
                    mutationSdl.append("  delete").append(typeName).append("(conditions: ").append(inputTypeName).append("!): String\n");
                    mutationFetchers.put("delete" + typeName,
                            env -> deleteData(api, env.getArgument("conditions")));
                }
            }
        }
        sdl.append("}\n\n");
        mutationSdl.append("}\n\n");

        // Generate Types and Input Types
        for (ApiDefinition api : apis) {
            if ("GraphQL".equalsIgnoreCase(api.getApiType())) {
                String tableName = api.getTableName();
                String typeName = capitalize(tableName);
                Map<String, Integer> columnTypes = schemaDiscoveryService.getColumnTypes(api.getConnection(), tableName);

                // Output type
                sdl.append("type ").append(typeName).append(" {\n");
                for (Map.Entry<String, Integer> entry : columnTypes.entrySet()) {
                    String gqlType = SchemaDiscoveryService.mapSqlTypeToGraphQL(entry.getValue());
                    sdl.append("  ").append(entry.getKey()).append(": ").append(gqlType).append("\n");
                }
                sdl.append("}\n");

                // Input type for mutations
                sdl.append("input ").append(typeName).append("Input {\n");
                for (Map.Entry<String, Integer> entry : columnTypes.entrySet()) {
                    String gqlType = SchemaDiscoveryService.mapSqlTypeToGraphQL(entry.getValue());
                    sdl.append("  ").append(entry.getKey()).append(": ").append(gqlType).append("\n");
                }
                sdl.append("}\n");
            }
        }

        if (queryFetchers.isEmpty() && mutationFetchers.isEmpty()) {
            sdl = new StringBuilder("type Query { hello: String }");
            queryFetchers.put("hello", env -> "World");
        } else {
            sdl.append(mutationSdl);
        }

        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl.toString());
        RuntimeWiring.Builder wiringBuilder = RuntimeWiring.newRuntimeWiring();
        if (!queryFetchers.isEmpty()) {
            wiringBuilder.type("Query", builder -> {
                queryFetchers.forEach(builder::dataFetcher);
                return builder;
            });
        }
        if (!mutationFetchers.isEmpty()) {
            wiringBuilder.type("Mutation", builder -> {
                mutationFetchers.forEach(builder::dataFetcher);
                return builder;
            });
        }

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, wiringBuilder.build());
        this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
        logger.info("GraphQL schema refreshed with {} API definitions", apis.size());
    }

    /**
     * Fetches data from the external database for a specific API definition.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchData(ApiDefinition api) {
        try {
            ObjectContext context = externalConnectivityService.getContext(api.getConnection());
            String columns = (api.getIncludedColumns() != null && !api.getIncludedColumns().isEmpty())
                    ? api.getIncludedColumns()
                    : "*";
            String sql = "SELECT " + columns + " FROM " + api.getTableName();

            List<DataRow> rows = SQLSelect.dataRowQuery(sql).select(context);

            List<Map<String, Object>> results = new ArrayList<>();
            for (DataRow row : rows) {
                results.add(new HashMap<>(row));
            }
            return results;
        } catch (Exception e) {
            logger.error("Error fetching GraphQL data for table {}", api.getTableName(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Inserts a row into the external database for a specific API definition.
     */
    private String insertData(ApiDefinition api, Map<String, Object> data) {
        try {
            ObjectContext context = externalConnectivityService.getContext(api.getConnection());
            Set<String> schemaColumns = new HashSet<>(
                    schemaDiscoveryService.getColumns(api.getConnection(), api.getTableName()));

            StringBuilder sql = new StringBuilder("INSERT INTO ").append(api.getTableName()).append(" (");
            StringBuilder values = new StringBuilder("VALUES (");
            List<Object> params = new ArrayList<>();

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (schemaColumns.stream().anyMatch(c -> c.equalsIgnoreCase(entry.getKey()))) {
                    sql.append(entry.getKey()).append(", ");
                    values.append("?, ");
                    params.add(entry.getValue());
                }
            }
            if (!params.isEmpty()) {
                sql.setLength(sql.length() - 2);
                values.setLength(values.length() - 2);
            }
            sql.append(") ").append(values).append(")");

            org.apache.cayenne.query.SQLExec.query(sql.toString())
                    .paramsArray(params.toArray())
                    .execute(context);
            return "Inserted successfully";
        } catch (Exception e) {
            logger.error("Error inserting GraphQL data into table {}", api.getTableName(), e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Updates rows in the external database for a specific API definition.
     */
    private String updateData(ApiDefinition api, Map<String, Object> data, Map<String, Object> conditions) {
        try {
            ObjectContext context = externalConnectivityService.getContext(api.getConnection());
            Set<String> schemaColumns = new HashSet<>(
                    schemaDiscoveryService.getColumns(api.getConnection(), api.getTableName()));

            StringBuilder sql = new StringBuilder("UPDATE ").append(api.getTableName()).append(" SET ");
            List<Object> params = new ArrayList<>();

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (schemaColumns.stream().anyMatch(c -> c.equalsIgnoreCase(entry.getKey()))) {
                    sql.append(entry.getKey()).append(" = ?, ");
                    params.add(entry.getValue());
                }
            }
            if (!params.isEmpty()) sql.setLength(sql.length() - 2);

            sql.append(" WHERE ");
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                if (schemaColumns.stream().anyMatch(c -> c.equalsIgnoreCase(entry.getKey()))) {
                    sql.append(entry.getKey()).append(" = ? AND ");
                    params.add(entry.getValue());
                }
            }
            sql.setLength(sql.length() - 5);

            org.apache.cayenne.query.SQLExec.query(sql.toString())
                    .paramsArray(params.toArray())
                    .execute(context);
            return "Updated successfully";
        } catch (Exception e) {
            logger.error("Error updating GraphQL data in table {}", api.getTableName(), e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Deletes rows from the external database for a specific API definition.
     */
    private String deleteData(ApiDefinition api, Map<String, Object> conditions) {
        try {
            ObjectContext context = externalConnectivityService.getContext(api.getConnection());
            Set<String> schemaColumns = new HashSet<>(
                    schemaDiscoveryService.getColumns(api.getConnection(), api.getTableName()));

            StringBuilder sql = new StringBuilder("DELETE FROM ").append(api.getTableName()).append(" WHERE ");
            List<Object> params = new ArrayList<>();

            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                if (schemaColumns.stream().anyMatch(c -> c.equalsIgnoreCase(entry.getKey()))) {
                    sql.append(entry.getKey()).append(" = ? AND ");
                    params.add(entry.getValue());
                }
            }
            sql.setLength(sql.length() - 5);

            org.apache.cayenne.query.SQLExec.query(sql.toString())
                    .paramsArray(params.toArray())
                    .execute(context);
            return "Deleted successfully";
        } catch (Exception e) {
            logger.error("Error deleting GraphQL data from table {}", api.getTableName(), e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param str the string to capitalize
     * @return the capitalized string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
