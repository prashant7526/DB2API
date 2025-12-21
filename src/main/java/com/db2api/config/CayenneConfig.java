package com.db2api.config;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

/**
 * Configuration class for Apache Cayenne.
 * Sets up the ServerRuntime used for dynamic database interactions.
 */
@Configuration
public class CayenneConfig {

    /**
     * Creates and configures the Apache Cayenne ServerRuntime.
     * 
     * @param dataSource the primary data source to be used by Cayenne
     * @return the configured ServerRuntime instance
     */
    @Bean
    public ServerRuntime serverRuntime(DataSource dataSource) {
        return ServerRuntime.builder()
                .addConfig("cayenne-project.xml")
                .dataSource(dataSource)
                .build();
    }
}
