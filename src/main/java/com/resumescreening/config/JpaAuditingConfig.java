package com.resumescreening.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Separate config class for JPA Auditing.
 * Keeping it separate from the main application class allows
 * @WebMvcTest slices to exclude it without breaking the context.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // Intentionally empty — annotation does the work
}
