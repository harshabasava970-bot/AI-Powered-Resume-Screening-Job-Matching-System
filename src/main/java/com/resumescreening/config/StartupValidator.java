package com.resumescreening.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Logs key configuration at startup so you can verify
 * environment variables are being picked up correctly on Render.
 */
@Configuration
public class StartupValidator {

    private static final Logger log = LoggerFactory.getLogger(StartupValidator.class);

    @Value("${spring.datasource.url:NOT_SET}")
    private String dbUrl;

    @Value("${spring.datasource.username:NOT_SET}")
    private String dbUsername;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Bean
    CommandLineRunner validateConfig() {
        return args -> {
            log.info("=== STARTUP CONFIGURATION CHECK ===");
            log.info("Active Profile : {}", activeProfile);
            log.info("Server Port    : {}", serverPort);
            log.info("DB URL         : {}", maskPassword(dbUrl));
            log.info("DB Username    : {}", dbUsername);
            log.info("DB URL is set  : {}", !"NOT_SET".equals(dbUrl));
            log.info("===================================");
        };
    }

    private String maskPassword(String url) {
        if (url == null || url.equals("NOT_SET")) return "NOT_SET";
        // Mask password in URL if present (e.g. jdbc:mysql://user:pass@host)
        return url.replaceAll(":[^:@/]+@", ":***@");
    }
}
