package com.shoplab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point. {@code @SpringBootApplication} tells Spring to scan this package (and sub-packages)
 * for controllers, services, repositories, etc., create them, and wire them together.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ShopLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopLabApplication.class, args);
    }
}
