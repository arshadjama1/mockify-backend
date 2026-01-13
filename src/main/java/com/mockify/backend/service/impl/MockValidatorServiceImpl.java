package com.mockify.backend.service.impl;

import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.service.MockValidatorService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class MockValidatorServiceImpl implements MockValidatorService {


    /**
     * All supported data types that a schema field is allowed to use.
     * This keeps the system strict and prevents users from defining random or unsafe types.
     */

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "string", "number", "boolean", "array", "object",
            "email", "uuid", "datetime", "null", "json", "enum"
    );

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    // validateSchemaDefinition

    @Override
    public void validateSchemaDefinition(Map<String, Object> schemaJson) {

        if (schemaJson == null || schemaJson.isEmpty()) {
            throw new BadRequestException("Schema JSON cannot be empty");
        }

        for (Map.Entry<String, Object> entry : schemaJson.entrySet()) {

            String field = entry.getKey();
            Object definition = entry.getValue();

            if (field == null || field.trim().isEmpty()) {
                throw new BadRequestException("Schema field name cannot be empty");
            }

            String type;


            if (definition instanceof String strType) {
                type = strType.toLowerCase();
            }
            else if (definition instanceof Map<?, ?> defMap) {

                Object typeObj = defMap.get("type");
                if (!(typeObj instanceof String)) {
                    throw new BadRequestException("Field '" + field + "' must define a type");
                }

                type = typeObj.toString().toLowerCase();

                if ("enum".equals(type)) {
                    Object values = defMap.get("values");
                    if (!(values instanceof List<?> list) || list.isEmpty()) {
                        throw new BadRequestException("Enum field '" + field + "' must define non-empty values");
                    }
                }
            }
            else {
                throw new BadRequestException("Invalid schema format for field '" + field + "'");
            }

            if (!ALLOWED_TYPES.contains(type)) {
                throw new BadRequestException("Invalid type for field '" + field + "'. Allowed: " + ALLOWED_TYPES);
            }
        }
    }


    // Record Validation

    @Override
    public void validateRecordAgainstSchema(
            Map<String, Object> schemaJson,
            Map<String, Object> recordJson
    ) {

        if (recordJson == null) {
            throw new BadRequestException("Record data cannot be null");
        }

        // Validate each field defined in schema

        for (Map.Entry<String, Object> entry : schemaJson.entrySet()) {

            String field = entry.getKey();
            Object schemaDef = entry.getValue();

            if (!recordJson.containsKey(field)) {
                throw new BadRequestException("Missing field '" + field + "' in record");
            }

            Object value = recordJson.get(field);

            String type;
            List<?> enumValues = null;

            if (schemaDef instanceof String strType) {
                type = strType.toLowerCase();
            }
            else if (schemaDef instanceof Map<?, ?> defMap) {
                type = defMap.get("type").toString().toLowerCase();

                if ("enum".equals(type)) {
                    enumValues = (List<?>) defMap.get("values");
                }
            }
            else {
                throw new BadRequestException("Invalid schema definition for field '" + field + "'");
            }

            validateValueByType(field, type, value, enumValues);
        }

        // Extra field protection
        for (String field : recordJson.keySet()) {
            if (!schemaJson.containsKey(field)) {
                throw new BadRequestException("Field '" + field + "' is not allowed in this schema");
            }
        }
    }


    // validateValueByType

    private void validateValueByType(String field, String type, Object value, List<?> enumValues) {

        if ("null".equals(type)) {
            if (value != null) {
                throw new BadRequestException("Field '" + field + "' must be null");
            }
            return;
        }

        if (value == null) {
            throw new BadRequestException("Field '" + field + "' cannot be null");
        }

        switch (type) {

            case "string" -> require(value instanceof String, field, "string");

            case "number" -> require(value instanceof Number, field, "number");

            case "boolean" -> require(value instanceof Boolean, field, "boolean");

            case "array" -> require(value instanceof List<?>, field, "array");

            case "object", "json" -> require(value instanceof Map<?, ?>, field, "object");

            case "email" -> {
                require(value instanceof String, field, "email");
                if (!EMAIL_PATTERN.matcher(value.toString()).matches()) {
                    throw new BadRequestException("Invalid email format for field '" + field + "'");
                }
            }

            case "uuid" -> {
                require(value instanceof String, field, "uuid");
                try {
                    UUID.fromString(value.toString());
                } catch (Exception e) {
                    throw new BadRequestException("Invalid UUID format for field '" + field + "'");
                }
            }

            case "datetime" -> {
                require(value instanceof String, field, "datetime");
                try {
                    OffsetDateTime.parse(value.toString());
                } catch (Exception e) {
                    throw new BadRequestException("Invalid datetime format (ISO-8601) for field '" + field + "'");
                }
            }

            case "enum" -> {
                if (enumValues == null || !enumValues.contains(value)) {
                    throw new BadRequestException(
                            "Invalid enum value for field '" + field + "'. Allowed: " + enumValues
                    );
                }
            }

            default -> throw new BadRequestException("Unsupported schema data type: " + type);
        }
    }

    private void require(boolean condition, String field, String type) {
        if (!condition) {
            throw new BadRequestException("Field '" + field + "' must be of type " + type);
        }
    }
}
