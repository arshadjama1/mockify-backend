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
 * OpenApiParserService
 *
 * Responsibility:
 * - Accept uploaded OpenAPI/Swagger file
 * - Parse JSON/YAML into OpenAPI object
 * - Detect OpenAPI version
 * - Extract schemas from:
 *      OpenAPI 3.x -> components.schemas
 *      Swagger 2.x -> definitions
 * - Normalize into ParsedOpenApiSpec
 *
 * This service does NOT:
 * - validate business rules
 * - create schemas
 * - save database records
 *
 * It only parses raw specification safely.
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

        // Validate file existence before parsing
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

            // Detect OpenAPI version
            String version = openAPI.getOpenapi();

            // If version missing, likely unsupported Swagger file
            if (version == null || version.isBlank()) {
                throw new InvalidOpenApiException(
                        "Unsupported specification. Only OpenAPI 3.x is supported in MVP"
                );
            }

            // Extract schemas from components section
            Map<String, Schema> schemas = extractSchemas(openAPI);

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
     * definitions (future enhancement)
     *
     * @param openAPI parsed OpenAPI object
     * @return schema map
     */
    private Map<String, Schema> extractSchemas(OpenAPI openAPI) {

        // Ensure components section exists
        if (openAPI.getComponents() == null ||
                openAPI.getComponents().getSchemas() == null) {

            log.warn("No schemas found inside OpenAPI components");
            return Collections.emptyMap();
        }

        return openAPI.getComponents().getSchemas();
    }
}
