package com.resumescreening;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration smoke test: verifies the Spring application context loads
 * successfully with H2 in-memory database and all auto-configurations.
 *
 * Uses WebEnvironment.RANDOM_PORT so the full MVC stack is available.
 * DataInitializer runs against H2, which is fine since H2 is in-memory.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("Application Context Loads")
class ResumeScreeningApplicationTest {

    @Test
    @DisplayName("Spring application context should load without errors")
    void contextLoads() {
        // Passes if Spring Boot starts successfully with H2 + all beans wired.
    }
}
