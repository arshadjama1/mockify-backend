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
                        new Tag().name("Admin").description("Administrative operations for reviewing and managing user observations."),
                        new Tag().name("Authentication").description("User authentication and authorization operations including registration, login, logout, token refresh, and current user retrieval."),
                        new Tag().name("Organization").description("Operations for managing organizations and their associated resources."),
                        new Tag().name("Project").description("Operations for managing the project lifecycle and related resources."),
                        new Tag().name("Mock Schema").description("Operations for defining and managing mock data schemas."),
                        new Tag().name("Mock Record").description("Operations for creating, retrieving, updating, and deleting mock records based on defined schemas."),
                        new Tag().name("Public MockRecord").description("Publicly accessible operations for retrieving mock records."),
                        new Tag().name("Endpoint").description("Operations for validating and resolving endpoint slugs.")
                )))
                .build();
    }
}