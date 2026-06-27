package com.mockify.backend.service.impl;

import com.mockify.backend.service.SchemaExtractorService;
import io.swagger.v3.oas.models.media.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SchemaExtractorService
 *
 * Responsibility:
 * - Convert validated OpenAPI schema properties
 *   into Mockify-compatible schemaJson format
 *
 * Example:
 *
 * OpenAPI:
 * User:
 *   properties:
 *     id:
 *       type: string
 *       format: uuid
 *     email:
 *       type: string
 *       format: email
 *
 * Output:
 * {
 *   "id": "uuid",
 *   "email": "email"
 * }
 *
 * This service:
 * - Maps OpenAPI primitive types
 * - Handles formats
 * - Supports nested objects
 * - Supports arrays
 * - Supports resolved $ref schemas
 *
 * This service does NOT:
 * - Validate OpenAPI spec
 * - Save DB records
 * - Create endpoints
 */
@Service
@Slf4j
public class SchemaExtractorServiceImpl implements SchemaExtractorService {

    /**
     * Extract complete schemaJson for one OpenAPI component
     *
     * @param schemaName component name
     * @param schema OpenAPI schema object
     * @return Mockify schema JSON structure
     */
    @Override
    public Map<String, Object> extractSchema(String schemaName, Schema<?> schema) {

        if (schema == null || schema.getProperties() == null) {
            throw new IllegalArgumentException(
                    "Schema '" + schemaName + "' contains no extractable properties"
            );
        }

        Map<String, Object> extracted = new LinkedHashMap<>();

        // Preserve field order from OpenAPI spec
        for (Map.Entry<String, Schema> property :
                (Iterable<Map.Entry<String, Schema>>) schema.getProperties().entrySet()) {

            String propertyName = property.getKey();
            Schema<?> propertySchema = property.getValue();

            extracted.put(
                    propertyName,
                    extractPropertyType(propertySchema)
            );
        }

        log.info(
                "Extracted schema '{}' with {} fields",
                schemaName,
                extracted.size()
        );

        return extracted;
    }

    /**
     * Extract property type recursively
     *
     * Handles:
     * - string
     * - integer
     * - boolean
     * - number
     * - array
     * - object
     * - $ref
     */
    private Object extractPropertyType(Schema<?> propertySchema) {

        /*
         * Handle resolved references
         *
         * Example:
         * author:
         *   $ref: '#/components/schemas/User'
         *
         * After parser resolution,
         * referenced schema behaves like normal object schema
         */
        if (propertySchema.get$ref() != null) {

            // Simplified MVP:
            // treat referenced schema as nested object
            return "object";
        }

        String type = propertySchema.getType();

        if (type == null) {

            // Sometimes parser resolves schemas
            // without direct type but with properties
            if (propertySchema.getProperties() != null) {
                return extractNestedObject(propertySchema);
            }

            return "string";
        }

        // Data types handling
        return switch (type) {

            case "string" -> mapStringType(propertySchema);

            case "integer", "number" -> "number";

            case "boolean" -> "boolean";

            case "array" -> extractArrayType(propertySchema);

            case "object" -> extractNestedObject(propertySchema);

            default -> "string";
        };
    }

    /**
     * Map string formats into Mockify-specific types
     *
     * OpenAPI:
     * - uuid
     * - email
     * - date-time
     */
    private String mapStringType(Schema<?> schema) {

        String format = schema.getFormat();

        if (format == null) {
            return "string";
        }

        return switch (format.toLowerCase()) {

            case "uuid" -> "uuid";

            case "email" -> "email";

            case "date-time", "datetime" -> "datetime";

            case "date" -> "date";

            case "uri", "url" -> "url";

            default -> "string";
        };
    }

    /**
     * Extract array structure
     *
     * Example:
     * tags:
     *   type: array
     *   items:
     *     type: string
     *
     * Output:
     * {
     *   "type": "array",
     *   "items": "string"
     * }
     */
    private Map<String, Object> extractArrayType(Schema<?> schema) {

        Map<String, Object> arrayDefinition = new LinkedHashMap<>();

        arrayDefinition.put("type", "array");

        Schema<?> itemSchema = schema.getItems();

        if (itemSchema == null) {

            // Default fallback
            arrayDefinition.put("items", "string");

            return arrayDefinition;
        }

        arrayDefinition.put(
                "items",
                extractPropertyType(itemSchema)
        );

        return arrayDefinition;
    }

    /**
     * Extract nested object recursively
     *
     * Example:
     * profile:
     *   type: object
     *   properties:
     *     age:
     *       type: integer
     *
     * Output:
     * {
     *   "age": "number"
     * }
     */
    private Map<String, Object> extractNestedObject(Schema<?> schema) {

        Map<String, Object> nested = new LinkedHashMap<>();

        if (schema.getProperties() == null || schema.getProperties().isEmpty()) {

            /*
             * Empty object fallback
             */
            nested.put("type", "object");
            return nested;
        }

        for (Map.Entry<String, Schema> property :
                (Iterable<Map.Entry<String, Schema>>) schema.getProperties().entrySet()) {

            nested.put(
                    property.getKey(),
                    extractPropertyType(property.getValue())
            );
        }

        return nested;
    }
}