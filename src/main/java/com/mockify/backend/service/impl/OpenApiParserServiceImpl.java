package com.mockify.backend.service.impl;

import com.mockify.backend.dto.internal.ParsedOpenApiSpec;
import com.mockify.backend.exception.InvalidOpenApiException;
import com.mockify.backend.service.OpenApiParserService;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Parse uploaded API specification into normalized internal ParsedOpenApiSpec.
 *
 * Supported:
 * - OpenAPI 3.x
 * - Swagger 2.0
 *
 * Features:
 * - YAML / JSON parsing
 * - Automatic $ref resolution
 * - Reusable schema extraction
 * - Legacy Swagger compatibility
 *
 * Security:
 * - Defensive null/empty validation
 * - Structural validation
 * - Safe parser failure handling
 *
 */
@Service
@Slf4j
public class OpenApiParserServiceImpl implements OpenApiParserService {

    /**
     * Parse uploaded OpenAPI file into internal ParsedOpenApiSpec
     *
     * @param file uploaded swagger/openapi file
     * @return normalized parsed specification
     */
    @Override
    public ParsedOpenApiSpec parse(MultipartFile file) {

        // Defensive validation
        if (file == null || file.isEmpty()) {
            throw new InvalidOpenApiException("Uploaded OpenAPI file is empty");
        }

        try {
            // Convert uploaded file bytes into string content
            // swagger-parser accepts raw YAML/JSON string
            String content = new String(file.getBytes());

            // Parse options:
            // resolve = true -> resolve $ref references automatically
            // flatten = false -> preserve structure
            ParseOptions options = new ParseOptions();
            options.setResolve(true);
            options.setFlatten(false);

            // Parse specification
            SwaggerParseResult result = new OpenAPIParser()
                    .readContents(content, null, options);

            // Collect parsing messages/errors
            List<String> messages = result.getMessages();

            // If parser returns errors, reject file
            if (messages != null && !messages.isEmpty()) {
                log.error("OpenAPI parsing failed: {}", messages);
                throw new InvalidOpenApiException(
                        "Invalid OpenAPI specification: " + String.join(", ", messages)
                );
            }

            OpenAPI openAPI = result.getOpenAPI();

            // Null means parser failed to construct valid spec
            if (openAPI == null) {
                throw new InvalidOpenApiException("Unable to parse OpenAPI specification");
            }

            /*
             * Detect specification version.
             *
             * OpenAPI 3:
             * openapi field
             *
             * Swagger 2:
             * parser may normalize,
             * so fallback to raw content detection.
             */
            String version;

            if (openAPI.getOpenapi() != null &&
                    !openAPI.getOpenapi().isBlank()) {

                version = openAPI.getOpenapi();

            } else if (content.contains("swagger: \"2.0\"") ||
                    content.contains("swagger: '2.0'") ||
                    content.contains("swagger: 2.0")) {

                version = "2.0";

            } else {

                throw new InvalidOpenApiException(
                        "Unsupported or unknown API specification version"
                );
            }

            /*
             * Extract reusable schemas:
             * - OpenAPI 3 -> components.schemas
             * - Swagger 2 -> definitions
             */
            Map<String, Schema> schemas = extractSchemas(openAPI, version);

            log.info(
                    "Successfully parsed OpenAPI file '{}' | version={} | schemas={}",
                    file.getOriginalFilename(),
                    version,
                    schemas.size()
            );

            // Return normalized internal DTO
            return new ParsedOpenApiSpec(
                    version,
                    schemas
            );

        } catch (IOException e) {
            log.error("Failed to read uploaded OpenAPI file", e);
            throw new InvalidOpenApiException("Failed to read OpenAPI file");
        } catch (Exception e) {
            log.error("Unexpected OpenAPI parsing failure", e);
            throw new InvalidOpenApiException("Failed to parse OpenAPI specification");
        }
    }

    /**
     * Extract reusable schemas/components from OpenAPI specification
     *
     * OpenAPI 3:
     * components.schemas
     *
     * Swagger 2:
     * definitions
     *
     * @param openAPI parsed OpenAPI object
     * @return schema map
     */
    private Map<String, Schema> extractSchemas(OpenAPI openAPI, String version) {

        /*
         * Primary extraction:
         * OpenAPI 3 components
         */
        if (openAPI.getComponents() != null &&
                openAPI.getComponents().getSchemas() != null) {

            return openAPI.getComponents().getSchemas();
        }

        /*
         * Swagger 2 fallback:
         * Parser often converts definitions automatically,
         * but if schemas are absent,
         * return empty map gracefully.
         */
        if ("2.0".equals(version)) {

            log.warn("Swagger 2.0 specification parsed, but no reusable definitions found");

            return Collections.emptyMap();
        }

        log.warn("No reusable schemas found inside specification");

        return Collections.emptyMap();
    }
}
