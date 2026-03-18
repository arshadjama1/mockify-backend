package com.mockify.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

/**
 * Configuration class for customizing pagination behavior in Spring.
 *
 * Sets a default fallback Pageable when no pagination parameters
 * (page, size) are provided in the request.
 *
 * Default behavior:
 * - Page number: 0 (first page)
 * - Page size: 10
 *
 * Helps ensure consistent pagination and avoids unbounded queries.
 */

@Configuration
public class PageableConfig {
    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer customize() {
        return resolver -> {
            resolver.setFallbackPageable(PageRequest.of(0, 10)); // default = 10
        };
    }
}
