package com.shoplab.config;

import com.shoplab.auth.CurrentUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ShopLabProperties properties;
    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    public WebConfig(ShopLabProperties properties, CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.properties = properties;
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    /**
     * CORS: browsers block JavaScript on one origin (e.g. http://localhost:3000, the frontend)
     * from calling another origin (http://localhost:8080, this API) unless the API allows it.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(properties.corsAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    /** Lets controller methods declare a {@code @CurrentUser User user} parameter. */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
