package com.shoplab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Our custom settings from application.yml (the "shoplab:" section), as a typed Java object.
 */
@ConfigurationProperties(prefix = "shoplab")
public record ShopLabProperties(
        boolean testEndpointsEnabled,
        List<String> corsAllowedOrigins
) {
}
