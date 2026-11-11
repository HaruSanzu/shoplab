package com.shoplab.config;

import com.shoplab.auth.CurrentUser;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI settings (http://localhost:8080/swagger-ui.html).
 * The "bearerAuth" scheme adds the "Authorize" button, where you paste a token from /api/auth/login.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "ShopLab API", version = "1.0",
        description = "Training e-commerce API for test automation practice"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer")
public class OpenApiConfig {

    static {
        // The @CurrentUser parameter is filled from the token, not by the caller: hide it from the docs.
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
    }
}
