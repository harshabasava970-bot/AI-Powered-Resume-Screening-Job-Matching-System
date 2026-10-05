package com.resumescreening.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Custom DataSource configuration.
 *
 * Handles both Aiven-style URLs (mysql://... or jdbc:mysql://...?ssl-mode=REQUIRED)
 * and standard JDBC URLs automatically.
 * This means you can paste the Aiven connection URI directly as DB_URL
 * and it will be converted to a valid JDBC URL automatically.
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${DB_URL:jdbc:mysql://localhost:3306/resume_screening_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}")
    private String rawDbUrl;

    @Value("${DB_USERNAME:root}")
    private String dbUsername;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        String jdbcUrl = normalizeUrl(rawDbUrl);
        log.info("Connecting to database: {}", maskUrl(jdbcUrl));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Conservative pool settings for free-tier cloud databases
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("ResumeAI-HikariPool");

        // Extra MySQL properties for Aiven compatibility
        config.addDataSourceProperty("serverTimezone", "UTC");
        config.addDataSourceProperty("allowPublicKeyRetrieval", "true");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return new HikariDataSource(config);
    }

    /**
     * Converts any supported URL format to a valid JDBC URL.
     *
     * Handles:
     * - mysql://user:pass@host:port/db?ssl-mode=REQUIRED  (Aiven URI format)
     * - jdbc:mysql://host:port/db?ssl-mode=REQUIRED       (Aiven JDBC with wrong SSL param)
     * - jdbc:mysql://host:port/db?useSSL=true             (standard JDBC — passed through)
     */
    String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("DB_URL environment variable is not set!");
        }

        // Step 1: Convert mysql:// URI to jdbc:mysql://
        // Aiven gives: mysql://avnadmin:PASS@host:port/defaultdb?ssl-mode=REQUIRED
        if (url.startsWith("mysql://")) {
            url = convertAivenUri(url);
        }

        // Step 2: Replace Aiven's ssl-mode=REQUIRED with proper JDBC SSL params
        if (url.contains("ssl-mode=REQUIRED") || url.contains("ssl-mode=require")) {
            url = url.replaceAll("[?&]ssl-mode=[A-Z_a-z]+", "");
            // Append proper JDBC SSL params
            url = appendParam(url, "useSSL=true");
            url = appendParam(url, "requireSSL=true");
        }

        // Step 3: Ensure essential params are present
        if (!url.contains("serverTimezone")) {
            url = appendParam(url, "serverTimezone=UTC");
        }
        if (!url.contains("allowPublicKeyRetrieval")) {
            url = appendParam(url, "allowPublicKeyRetrieval=true");
        }

        return url;
    }

    /**
     * Convert Aiven URI format: mysql://user:pass@host:port/db?params
     * to JDBC format:           jdbc:mysql://host:port/db?params
     * Username/password are extracted and set separately via setUsername/setPassword.
     */
    private String convertAivenUri(String uri) {
        // mysql://avnadmin:AVNS_xxx@mysql-xxx.aivencloud.com:14858/defaultdb?ssl-mode=REQUIRED
        String withoutScheme = uri.substring("mysql://".length());

        // Split userinfo@hostinfo
        int atIndex = withoutScheme.indexOf('@');
        if (atIndex < 0) {
            // No credentials in URL — just convert scheme
            return "jdbc:mysql://" + withoutScheme;
        }

        // Extract credentials — but we'll use the configured username/password,
        // so we just strip the credentials from the URL
        String hostAndRest = withoutScheme.substring(atIndex + 1);
        return "jdbc:mysql://" + hostAndRest;
    }

    private String appendParam(String url, String param) {
        return url.contains("?") ? url + "&" + param : url + "?" + param;
    }

    private String maskUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll(":[^:@/?&]+@", ":***@")
                  .replaceAll("password=[^&]+", "password=***");
    }
}
