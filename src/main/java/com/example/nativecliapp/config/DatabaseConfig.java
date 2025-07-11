package com.example.nativecliapp.config;

import com.example.nativecliapp.constant.DatabaseConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "database")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseConfig {

    @Builder.Default
    private int maxPoolSize = DatabaseConstants.DEFAULT_MAX_POOL_SIZE;

    @Builder.Default
    private int minIdle = DatabaseConstants.DEFAULT_MIN_IDLE;

    @Builder.Default
    private long connectionTimeout = DatabaseConstants.DEFAULT_CONNECTION_TIMEOUT;

    @Builder.Default
    private long idleTimeout = DatabaseConstants.DEFAULT_IDLE_TIMEOUT;

    @Builder.Default
    private long maxLifetime = DatabaseConstants.DEFAULT_MAX_LIFETIME;

    @Builder.Default
    private boolean enableMetrics = true;

    @Builder.Default
    private String healthCheckQuery = "SELECT 1";

    @Valid
    private DefaultConnection defaultConnection;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefaultConnection {
        @NotBlank
        private String url = "jdbc:h2:mem:testdb";

        @NotBlank
        private String username = "sa";

        private String password = "";

        @Builder.Default
        private String type = DatabaseConstants.DB_TYPE_H2;
    }
}
