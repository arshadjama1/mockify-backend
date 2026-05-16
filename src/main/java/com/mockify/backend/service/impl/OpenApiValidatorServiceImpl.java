package com.mockify.backend.service.impl;

import com.mockify.backend.dto.internal.ParsedOpenApiSpec;
import com.mockify.backend.exception.InvalidOpenApiException;
import com.mockify.backend.service.OpenApiValidatorService;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Set;

/**
 * OpenApiValidatorService
 *
 * Responsibility:
 * - Validate already parsed OpenAPI specification
 * - Ensure required schema structure exists
 * - Reject unsupported formats early
 * - Prevent invalid schema imports before DB creation
 *
 * This service validates:
 * - OpenAPI version
 * - Components existence
 * - Schema definitions
 * - Schema names
 * - Property presence
 * - Supported field types
 *
 * This service does NOT:
 * - Parse raw files
 * - Convert schema fields
 * - Save mock schemas
 */
@Service
@Slf4j
public class OpenApiValidatorServiceImpl implements OpenApiValidatorService {

    /**
     * Supported primitive OpenAPI types for MVP
     *
     * Future:
     * - oneOf
     * - anyOf
     * - allOf
     * - enum
     * - advanced refs
     */
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "string",
            "integer",
            "number",
            "boolean",
            "array",
            "object"
    );

    /**
     * Validate entire parsed OpenAPI specification
     *
     * @param spec normalized parsed spec
     */
    public void validate(ParsedOpenApiSpec spec) {

        // Basic null check
        if (spec == null) {
            throw new InvalidOpenApiException("Parsed OpenAPI specification is null");
        }

        validateVersion(spec.getVersion());
        validateSchemasExist(spec.getSchemas());
        validateEachSchema(spec.getSchemas());

        log.info(
                "OpenAPI validation successful | version={} | schemas={}",
                spec.getVersion(), spec.getSchemas().size()
        );
    }

    /**
     * Validate supported OpenAPI version
     *
     * MVP supports:
     * - OpenAPI 3.x
     * - Swagger 2.0
     *
     * Future:
     * JSON
     */
    private void validateVersion(String version) {

        if (version == null || version.isBlank()) {
            throw new InvalidOpenApiException("Missing OpenAPI version");
        }

        // Accept only 3.x specs
        if (!version.startsWith("3")) {
            throw new InvalidOpenApiException(
                    "Unsupported OpenAPI version: " + version +
                            ". Only OpenAPI 3.x is supported"
            );
        }
    }

    /**
     * Ensure schemas/components section exists
     */
    private void validateSchemasExist(Map<String, Schema> schemas) {

        if (schemas == null || schemas.isEmpty()) {
            throw new InvalidOpenApiException(
                    "No reusable schemas found in OpenAPI components"
            );
        }
    }

    /**
     * Validate all schema components
     */
    private void validateEachSchema(Map<String, Schema> schemas) {

        for (Map.Entry<String, Schema> entry : schemas.entrySet()) {

            String schemaName = entry.getKey();
            Schema schema = entry.getValue();

            validateSchemaName(schemaName);
            validateSchemaStructure(schemaName, schema);
        }
    }

    /**
     * Validate schema component name
     *
     * Example:
     * User
     * Product
     * BlogPost
     */
    private void validateSchemaName(String schemaName) {

        if (schemaName == null || schemaName.isBlank()) {
            throw new InvalidOpenApiException("Schema name cannot be empty");
        }

        // Prevent dangerous or malformed names
        if (schemaName.length() > 100) {
            throw new InvalidOpenApiException(
                    "Schema name too long: " + schemaName
            );
        }
    }

    /**
     * Validate schema structure
     *
     * MVP expects:
     * type: object
     * properties: {}
     */
    private void validateSchemaStructure(String schemaName, Schema schema) {

        if (schema == null) {
            throw new InvalidOpenApiException(
                    "Schema definition missing for component: " + schemaName
            );
        }

        // Root schema must contain properties
        if (schema.getProperties() == null || schema.getProperties().isEmpty()) {
            throw new InvalidOpenApiException(
                    "Schema '" + schemaName + "' contains no properties"
            );
        }

        // Validate each property
        for (Object propertyKey : schema.getProperties().keySet()) {

            String propertyName = propertyKey.toString();
            Schema propertySchema =
                    (Schema) schema.getProperties().get(propertyKey);

            validateProperty(schemaName, propertyName, propertySchema);
        }
    }

    /**
     * Validate individual property
     */
    private void validateProperty(
            String schemaName,
            String propertyName,
            Schema propertySchema
    ) {

        if (propertyName == null || propertyName.isBlank()) {
            throw new InvalidOpenApiException(
                    "Schema '" + schemaName + "' contains invalid property name"
            );
        }

        if (propertySchema == null) {
            throw new InvalidOpenApiException(
                    "Property '" + propertyName +
                            "' in schema '" + schemaName +
                            "' is missing definition"
            );
        }

        /*
         * $ref schemas may not contain direct type
         * Allow them for parser resolution
         */
        if (propertySchema.get$ref() != null) {
            return;
        }

        String type = propertySchema.getType();

        // Type required
        if (type == null || type.isBlank()) {
            throw new InvalidOpenApiException(
                    "Property '" + propertyName +
                            "' in schema '" + schemaName +
                            "' is missing type"
            );
        }

        // Reject unsupported types
        if (!SUPPORTED_TYPES.contains(type)) {
            throw new InvalidOpenApiException(
                    "Unsupported property type '" + type +
                            "' in schema '" + schemaName +
                            "', property '" + propertyName + "'"
            );
        }

        /*
         * Array validation:
         * Ensure item type exists
         */
        if ("array".equals(type)) {

            if (propertySchema.getItems() == null) {
                throw new InvalidOpenApiException(
                        "Array property '" + propertyName +
                                "' in schema '" + schemaName +
                                "' is missing item definition"
                );
            }
        }

        /*
         * Object validation:
         * Nested object allowed
         */
        if ("object".equals(type)) {

            // Empty nested object allowed for MVP
            // Future can recursively validate nested objects
        }
    }
}