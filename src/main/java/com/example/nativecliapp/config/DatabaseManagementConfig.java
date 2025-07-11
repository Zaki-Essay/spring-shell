package com.example.nativecliapp.config;

import com.example.nativecliapp.constant.DatabaseConstants;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Configuration
@EnableConfigurationProperties(DatabaseConfig.class)
@Slf4j
public class DatabaseManagementConfig {

    @Bean
    @ConditionalOnMissingBean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public DatabaseConfig databaseConfig() {
        return DatabaseConfig.builder()
                .defaultConnection(DatabaseConfig.DefaultConnection.builder()
                        .url("jdbc:h2:mem:testdb")
                        .username("sa")
                        .password("")
                        .type(DatabaseConstants.DB_TYPE_H2)
                        .build())
                .build();
    }

    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        log.info("🚀 Database Management System initialized successfully");
    }
}
