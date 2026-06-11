package com.db2api.service.connection;

import com.db2api.service.EncryptionService;
import com.db2api.persistent.connection.DbConnection;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.datasource.DataSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing connections to external databases.
 * Uses HikariCP connection pooling for efficient resource utilization
 * and caches ServerRuntime instances per connection.
 */
@Service
public class ExternalConnectivityService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalConnectivityService.class);

    private final Map<Long, ServerRuntime> runtimeCache = new ConcurrentHashMap<>();
    private final EncryptionService encryptionService;

    @Value("${app.external-db.pool.maximum-pool-size:5}")
    private int maximumPoolSize;

    @Value("${app.external-db.pool.minimum-idle:1}")
    private int minimumIdle;

    @Value("${app.external-db.pool.idle-timeout-ms:300000}")
    private long idleTimeoutMs;

    @Value("${app.external-db.pool.max-lifetime-ms:600000}")
    private long maxLifetimeMs;

    @Value("${app.external-db.pool.connection-timeout-ms:30000}")
    private long connectionTimeoutMs;

    /**
     * Constructs the ExternalConnectivityService with the encryption service.
     *
     * @param encryptionService the service for decrypting connection passwords
     */
    public ExternalConnectivityService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    /**
     * Gets an ObjectContext for the given database connection.
     *
     * @param connection the database connection configuration
     * @return a new ObjectContext for executing queries
     */
    public ObjectContext getContext(DbConnection connection) {
        return getRuntime(connection).newContext();
    }

    /**
     * Gets or creates a cached ServerRuntime for the given connection.
     *
     * @param connection the database connection configuration
     * @return the ServerRuntime for the connection
     */
    public ServerRuntime getRuntime(DbConnection connection) {
        return runtimeCache.computeIfAbsent(connection.getId(), id -> createRuntime(connection));
    }

    /**
     * Invalidates and shuts down the ServerRuntime for the given connection ID.
     * Should be called when a connection is updated or deleted.
     *
     * @param connectionId the ID of the connection to invalidate
     */
    public void invalidateRuntime(Long connectionId) {
        ServerRuntime runtime = runtimeCache.remove(connectionId);
        if (runtime != null) {
            runtime.shutdown();
        }
    }

    /**
     * Creates a new ServerRuntime with HikariCP connection pooling for the given connection.
     *
     * @param connection the database connection configuration
     * @return a new ServerRuntime with pooled data source
     */
    private ServerRuntime createRuntime(DbConnection connection) {
        String decryptedPassword = encryptionService.decrypt(connection.getPassword());

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(connection.getUrl());
        hikariConfig.setDriverClassName(connection.getDriverClass());
        hikariConfig.setUsername(connection.getUsername());
        hikariConfig.setPassword(decryptedPassword);
        hikariConfig.setMaximumPoolSize(maximumPoolSize);
        hikariConfig.setMinimumIdle(minimumIdle);
        hikariConfig.setIdleTimeout(idleTimeoutMs);
        hikariConfig.setMaxLifetime(maxLifetimeMs);
        hikariConfig.setConnectionTimeout(connectionTimeoutMs);
        hikariConfig.setPoolName("db2api-ext-" + connection.getId());

        HikariDataSource dataSource = new HikariDataSource(hikariConfig);

        logger.info("Created HikariCP pool for connection '{}' (id={}) with maxPoolSize={}",
                connection.getName(), connection.getId(), maximumPoolSize);

        return ServerRuntime.builder()
                .dataSource(dataSource)
                .build();
    }
}
