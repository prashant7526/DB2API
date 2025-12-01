package com.db2api.config;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class CayenneConfig {

    @Bean
    public ServerRuntime serverRuntime(DataSource dataSource) {
        return ServerRuntime.builder()
                .addConfig("cayenne-project.xml")
                .dataSource(dataSource)
                .build();
    }
}
