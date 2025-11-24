package com.mockify.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;


@OpenAPIDefinition
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI api() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mockify API")
                        .description("API documentation for Mockify backend service")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    // Swagger UI will show exactly this order
    @Bean
    public GroupedOpenApi mockifyApi() {
        return GroupedOpenApi.builder()
                .group("mockify-api")
                .pathsToMatch("/api/**")
                .packagesToScan("com.mockify.backend.controller")
                .addOpenApiCustomizer(openApi -> openApi.setTags(List.of(
                        new Tag().name("Authentication").description("Handles user registration, login, logout, token refresh, and current user retrieval."),
                        new Tag().name("Organization").description("Manages organization resources."),
                        new Tag().name("Project").description("Handles project lifecycle operations."),
                        new Tag().name("Mock Schema").description("Defines schema for mock data."),
                        new Tag().name("Mock Record").description("Manages mock records based on schemas.")
                )))
                .build();
    }
}