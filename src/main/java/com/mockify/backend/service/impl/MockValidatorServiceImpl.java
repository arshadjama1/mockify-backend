package com.mockify.backend.service.impl;

import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.service.MockValidatorService;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class MockValidatorServiceImpl implements MockValidatorService {


    /**
     * All supported data types that a schema field is allowed to use.
     * This keeps the system strict and prevents users from defining random or unsafe types.
     */

    private enum ALLOWED_TYPES {

        STRING("string"),
        NUMBER("number"),
        BOOLEAN("boolean"),
        ARRAY("array"),
        OBJECT("object"),
        EMAIL("email"),
        UUID("uuid"),
        DATE("date"),
        DATETIME("datetime"),
        NULL("null"),
        JSON("json"),
        ENUM("enum"),
        URL("url");

        private final String value;

        ALLOWED_TYPES(String value) {
            this.value = value;
        }

        public static ALLOWED_TYPES from(String value) {
            for (ALLOWED_TYPES type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new BadRequestException("Invalid schema type: " + value);
        }
    }


    /**
     * SCHEMA VALIDATION
     *
     * Supports:
     * - Primitive fields
     * - Arrays
     * - Nested objects
     * - JSON objects
     * - Enums
     */
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

            /*
             * SIMPLE TYPE:
             * "name": "string"
             */
            if (definition instanceof String strType) {

                parseType(field, strType);
                continue;
            }

            /*
             * COMPLEX FIELD:
             * object / array / enum / nested object
             */
            if (definition instanceof Map<?, ?> defMap) {

                Object typeObj = defMap.get("type");

                /*
                 * NESTED OBJECT SUPPORT:
                 *
                 * Example:
                 * "profile": {
                 *    "age": "number",
                 *    "gender": "string"
                 * }
                 *
                 * No explicit "type" means recursive object
                 */
                if (typeObj == null) {

                    validateSchemaDefinition(castToStringObjectMap(defMap));
                    continue;
                }

                if (!(typeObj instanceof String)) {
                    throw new BadRequestException(
                            "Field '" + field + "' must define a valid type"
                    );
                }

                ALLOWED_TYPES type = parseType(field, typeObj.toString());

                /*
                 * ENUM validation
                 */
                if (type == ALLOWED_TYPES.ENUM) {

                    Object values = defMap.get("values");

                    if (!(values instanceof List<?> list) || list.isEmpty()) {
                        throw new BadRequestException(
                                "Enum field '" + field +
                                        "' must define non-empty values"
                        );
                    }
                }

                /*
                 * ARRAY validation
                 */
                if (type == ALLOWED_TYPES.ARRAY) {

                    Object items = defMap.get("items");

                    if (items == null) {
                        throw new BadRequestException(
                                "Array field '" + field +
                                        "' must define items"
                        );
                    }

                    if (items instanceof String str) {
                        parseType(field + ".items", str);
                    } else if (items instanceof Map<?, ?> map) {
                        validateSchemaDefinition(
                                castToStringObjectMap(map)
                        );
                    } else {
                        throw new BadRequestException(
                                "Array field '" + field +
                                        "' must define valid type"
                        );
                    }
                }

                /*
                 * OBJECT / JSON recursive validation
                 */
                if (type == ALLOWED_TYPES.OBJECT ||
                        type == ALLOWED_TYPES.JSON) {

                    for (Map.Entry<?, ?> nestedEntry : defMap.entrySet()) {

                        String nestedField = nestedEntry.getKey().toString();

                        if ("type".equals(nestedField)) {
                            continue;
                        }

                        validateNestedDefinition(
                                field,
                                nestedField,
                                nestedEntry.getValue()
                        );
                    }
                }

                continue;
            }

            throw new BadRequestException(
                    "Invalid schema format for field '" + field + "'"
            );
        }
    }


    /**
     * RECORD VALIDATION
     *
     * Validates user records against schema,
     * including nested object structures.
     */
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
                throw new BadRequestException(
                        "Missing field '" + field + "' in record"
                );
            }

            Object value = recordJson.get(field);

            /*
             * SIMPLE TYPE
             */
            if (schemaDef instanceof String strType) {

                ALLOWED_TYPES type = parseType(field, strType);
                validateValueByType(field, type, value, null);

                continue;
            }

            /*
             * COMPLEX TYPE
             */
            if (schemaDef instanceof Map<?, ?> defMap) {

                Object typeObj = defMap.get("type");

                /*
                 * NESTED OBJECT
                 */
                if (typeObj == null) {

                    require(
                            value instanceof Map<?, ?>,
                            field,
                            "object"
                    );

                    validateRecordAgainstSchema(
                            castToStringObjectMap(defMap),
                            castToStringObjectMap((Map<?, ?>) value)
                    );

                    continue;
                }

                if (!(typeObj instanceof String)) {
                    throw new BadRequestException(
                            "Field '" + field +
                                    "' schema type must be string"
                    );
                }

                ALLOWED_TYPES type =
                        parseType(field, typeObj.toString());

                List<?> enumValues = null;

                if (type == ALLOWED_TYPES.ENUM) {
                    enumValues = (List<?>) defMap.get("values");
                }

                /*
                 * ARRAY validation
                 */
                if (type == ALLOWED_TYPES.ARRAY) {

                    require(
                            value instanceof List<?>,
                            field,
                            "array"
                    );

                    Object itemsDef = defMap.get("items");

                    if (itemsDef == null) {
                        throw new BadRequestException(
                                "Array field '" + field + "' must define items"
                        );
                    }

                    List<?> list = (List<?>) value;

                    for (int i = 0; i < list.size(); i++) {

                        Object itemValue = list.get(i);
                        String itemField = field + "[" + i + "]";

                        /*
                         * Array of primitive types
                         */
                        if (itemsDef instanceof String itemTypeStr) {

                            ALLOWED_TYPES itemType =
                                    parseType(itemField, itemTypeStr);

                            validateValueByType(
                                    itemField,
                                    itemType,
                                    itemValue,
                                    null
                            );

                            continue;
                        }

                        /*
                         * Array of objects / nested schemas
                         */
                        if (itemsDef instanceof Map<?, ?> itemMap) {

                            require(
                                    itemValue instanceof Map<?, ?>,
                                    itemField,
                                    "object"
                            );

                            Map<String, Object> itemSchema =
                                    castToStringObjectMap(itemMap);

                            Object itemType = itemSchema.get("type");

                            if (itemType == null) {

                                validateRecordAgainstSchema(
                                        itemSchema,
                                        castToStringObjectMap(
                                                (Map<?, ?>) itemValue
                                        )
                                );

                                continue;
                            }

                            if ("object".equalsIgnoreCase(itemType.toString()) ||
                                    "json".equalsIgnoreCase(itemType.toString())) {

                                validateRecordAgainstSchema(
                                        extractNestedSchema(itemSchema),
                                        castToStringObjectMap(
                                                (Map<?, ?>) itemValue
                                        )
                                );

                                continue;
                            }

                            throw new BadRequestException(
                                    "Array field '" + field +
                                            "' supports only object schemas in items"
                            );
                        }

                        throw new BadRequestException(
                                "Invalid items definition for array field '" +
                                        field + "'"
                        );
                    }

                    continue;
                }

                /*
                 * OBJECT recursive validation
                 */
                if (type == ALLOWED_TYPES.OBJECT ||
                        type == ALLOWED_TYPES.JSON) {

                    require(
                            value instanceof Map<?, ?>,
                            field,
                            "object"
                    );

                    Map<String, Object> nestedSchema =
                            extractNestedSchema(defMap);

                    validateRecordAgainstSchema(
                            nestedSchema,
                            castToStringObjectMap((Map<?, ?>) value)
                    );

                    continue;
                }

                validateValueByType(
                        field,
                        type,
                        value,
                        enumValues
                );

                continue;
            }

            throw new BadRequestException(
                    "Invalid schema definition for field '" + field + "'"
            );
        }

        /*
         * Extra field protection
         */
        for (String field : recordJson.keySet()) {
            if (!schemaJson.containsKey(field)) {
                throw new BadRequestException(
                        "Field '" + field +
                                "' is not allowed in this schema"
                );
            }
        }
    }


    /**
     * VALUE TYPE VALIDATION
     */
    private void validateValueByType(
            String field,
            ALLOWED_TYPES type,
            Object value,
            List<?> enumValues
    ) {

        if (type == ALLOWED_TYPES.NULL) {

            if (value != null) {
                throw new BadRequestException(
                        "Field '" + field + "' must be null"
                );
            }

            return;
        }

        if (value == null) {
            throw new BadRequestException(
                    "Field '" + field + "' cannot be null"
            );
        }

        switch (type) {

            case STRING:
                require(value instanceof String, field, "string");
                break;

            case NUMBER:
                require(value instanceof Number, field, "number");
                break;

            case BOOLEAN:
                require(value instanceof Boolean, field, "boolean");
                break;

            case ARRAY:
                require(value instanceof List<?>, field, "array");
                break;

            case OBJECT:
            case JSON:
                require(value instanceof Map<?, ?>, field, "object");
                break;

            case EMAIL:
                require(value instanceof String, field, "email");

                if (!EmailValidator.getInstance().isValid(value.toString())) {
                    throw new BadRequestException(
                            "Invalid email format for field '" + field + "'"
                    );
                }
                break;

            case UUID:
                require(value instanceof String, field, "uuid");

                try {
                    UUID.fromString(value.toString());
                } catch (Exception e) {
                    throw new BadRequestException(
                            "Invalid UUID format for field '" + field + "'"
                    );
                }
                break;


            case DATE:
                require(value instanceof String, field, "date");

                try {
                    LocalDate.parse(value.toString());
                } catch (Exception e) {
                    throw new BadRequestException(
                            "Invalid date format (yyyy-MM-dd) for field '"
                                    + field + "'"
                    );
                }
                break;

            case DATETIME:
                require(value instanceof String, field, "datetime");

                try {
                    OffsetDateTime.parse(value.toString());
                } catch (Exception e) {
                    throw new BadRequestException(
                            "Invalid datetime format for field '" + field + "'"
                    );
                }
                break;

            case URL:
                require(value instanceof String, field, "url");

                try {
                    URI uri = new URI(value.toString());

                    if (uri.getScheme() == null ||
                            uri.getHost() == null) {
                        throw new BadRequestException(
                                "Invalid URL format for field '" + field + "'"
                        );
                    }
                } catch (Exception e) {
                    throw new BadRequestException(
                            "Invalid URL format for field '" + field + "'"
                    );
                }
                break;

            case ENUM:
                if (enumValues == null || !enumValues.contains(value)) {
                    throw new BadRequestException(
                            "Invalid enum value for field '" +
                                    field +
                                    "'. Allowed: " +
                                    enumValues
                    );
                }
                break;
        }
    }


    /**
     * Validate nested field definitions recursively
     */
    private void validateNestedDefinition(
            String parentField,
            String nestedField,
            Object nestedDef
    ) {

        if (nestedDef instanceof String nestedType) {
            parseType(parentField + "." + nestedField, nestedType);
            return;
        }

        if (nestedDef instanceof Map<?, ?> nestedMap) {

            Map<String, Object> wrapped = new LinkedHashMap<>();
            wrapped.put(
                    nestedField,
                    castToStringObjectMap(nestedMap)
            );

            validateSchemaDefinition(wrapped);
            return;
        }

        throw new BadRequestException(
                "Nested field '" + nestedField +
                        "' inside '" + parentField +
                        "' has invalid definition"
        );
    }


    /**
     * Extract nested schema by removing "type"
     */
    private Map<String, Object> extractNestedSchema(
            Map<?, ?> defMap
    ) {

        Map<String, Object> nested = new HashMap<>();

        for (Map.Entry<?, ?> entry : defMap.entrySet()) {

            String key = entry.getKey().toString();

            if ("type".equals(key)) {
                continue;
            }

            nested.put(key, entry.getValue());
        }

        return nested;
    }


    /**
     * Generic safe casting helper
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> castToStringObjectMap(
            Map<?, ?> map
    ) {

        return (Map<String, Object>) map;
    }


    /**
     * Validation helper
     */
    private void require(
            boolean condition,
            String field,
            String type
    ) {

        if (!condition) {
            throw new BadRequestException(
                    "Field '" + field +
                            "' must be of type " + type
            );
        }
    }


    /**
     * Type parser helper
     */
    private ALLOWED_TYPES parseType(
            String field,
            String typeStr
    ) {

        try {
            return ALLOWED_TYPES.from(typeStr);
        } catch (Exception e) {
            throw new BadRequestException(
                    "Invalid type for field '" +
                            field +
                            "': " +
                            typeStr
            );
        }
    }
}
