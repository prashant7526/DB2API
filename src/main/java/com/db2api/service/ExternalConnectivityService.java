package com.db2api.service;

import com.db2api.persistent.DbConnection;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.datasource.DataSourceBuilder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExternalConnectivityService {

    private final Map<Long, ServerRuntime> runtimeCache = new ConcurrentHashMap<>();
    private final EncryptionService encryptionService;

    public ExternalConnectivityService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public ObjectContext getContext(DbConnection connection) {
        return getRuntime(connection).newContext();
    }

    public ServerRuntime getRuntime(DbConnection connection) {
        return runtimeCache.computeIfAbsent(connection.getId(), id -> createRuntime(connection));
    }

    public void invalidateRuntime(Long connectionId) {
        ServerRuntime runtime = runtimeCache.remove(connectionId);
        if (runtime != null) {
            runtime.shutdown();
        }
    }

    private ServerRuntime createRuntime(DbConnection connection) {
        String decryptedPassword = encryptionService.decrypt(connection.getPassword());

        DataSource dataSource = DataSourceBuilder
                .url(connection.getUrl())
                .driver(connection.getDriverClass())
                .userName(connection.getUsername())
                .password(decryptedPassword)
                .build();

        return ServerRuntime.builder()
                .dataSource(dataSource)
                .build();
    }
}
