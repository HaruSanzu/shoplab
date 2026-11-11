package com.shoplab.testsupport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints that exist ONLY to help automated tests.
 * Tests must not depend on each other, so each test can start from the same clean data:
 * POST /api/test/reset deletes everything and re-runs all Flyway migrations
 * (fresh tables, original 25 products, 3 built-in users).
 * Turned off with SHOPLAB_TEST_ENDPOINTS=false. A real shop would never ship this.
 */
@RestController
@RequestMapping("/api/test")
@ConditionalOnProperty(name = "shoplab.test-endpoints-enabled", havingValue = "true")
@Tag(name = "Test support", description = "Helpers for automated tests (disabled in 'production')")
public class TestDataController {

    private final Flyway flyway;

    public TestDataController(Flyway flyway) {
        this.flyway = flyway;
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset the database to the initial test data")
    public synchronized Map<String, String> reset() {
        flyway.clean();
        flyway.migrate();
        return Map.of("status", "Database reset to initial test data");
    }
}
