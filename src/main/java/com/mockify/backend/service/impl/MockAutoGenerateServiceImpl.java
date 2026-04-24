package com.mockify.backend.service.impl;

import com.github.javafaker.Faker;
import com.mockify.backend.service.MockAutoGenerateService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Supplier;

@Service
public class MockAutoGenerateServiceImpl implements MockAutoGenerateService {

    private final Faker faker = new Faker();
    private final Random random = new Random();

    // Exact field generators (base definitions)
    private final Map<String, Supplier<Object>> fieldGenerators = Map.of(
            "email", () -> faker.internet().emailAddress(),
            "username", () -> faker.name().username(),
            "id", () -> faker.number().numberBetween(1, 100000),
            "name", () -> faker.name().fullName()
    );

    // Type-based generators
    private final Map<String, Supplier<Object>> typeGenerators = Map.of(
            "string", () -> faker.lorem().word(),
            "number", () -> faker.number().numberBetween(1, 1000),
            "boolean", () -> faker.bool().bool(),
            "array", () -> List.of(faker.lorem().word(), faker.number().randomDigit()),
            "object", () -> Map.of("value", faker.lorem().word())
    );

    @Override
    public Map<String, Object> generateRecord(Map<String, Object> schemaJson) {
        Map<String, Object> record = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : schemaJson.entrySet()) {

            String field = entry.getKey();
            Object schemaDef = entry.getValue();

            ParsedSchema parsed = parseSchema(field, schemaDef);

            Supplier<Object> generator = resolveFieldGenerator(field)
                    .orElseGet(() -> resolveTypeGenerator(parsed.type(), parsed.enumValues()));

            record.put(field, generator.get());
        }

        return record;
    }

    /**
     * Parse schema definition safely
     */
    private ParsedSchema parseSchema(String field, Object schemaDef) {

        if (schemaDef instanceof String s) {
            return new ParsedSchema(s.toLowerCase(), null);
        }

        if (schemaDef instanceof Map<?, ?> defMap) {

            Object typeObj = defMap.get("type");
            if (!(typeObj instanceof String typeStr)) {
                throw new IllegalArgumentException(
                        "Missing or invalid 'type' for field: " + field
                );
            }

            List<?> enumValues = null;
            Object valuesObj = defMap.get("values");
            if (valuesObj instanceof List<?>) {
                enumValues = (List<?>) valuesObj;
            }

            return new ParsedSchema(typeStr.toLowerCase(), enumValues);
        }

        throw new IllegalArgumentException(
                "Invalid schema format for field: " + field
        );
    }

    /**
     * Try to resolve generator based on field name (fuzzy matching)
     */
    private Optional<Supplier<Object>> resolveFieldGenerator(String field) {
        String f = field.toLowerCase();


        if (f.contains("email")) return Optional.ofNullable(fieldGenerators.get("email"));
        if (f.contains("username")) return Optional.ofNullable(fieldGenerators.get("username"));
        if (f.equals("id") || f.endsWith("id")) return Optional.ofNullable(fieldGenerators.get("id"));
        if (f.contains("name")) return Optional.ofNullable(fieldGenerators.get("name"));


        return Optional.empty();
    }

    /**
     * Resolve generator based on type or throw clear error
     */
    /**
     * Type-based generator with ENUM support
     */
    private Supplier<Object> resolveTypeGenerator(String type, List<?> enumValues) {

        if ("enum".equals(type)) {
            if (enumValues == null || enumValues.isEmpty()) {
                throw new IllegalArgumentException("ENUM type requires non-empty values list");
            }
            return () -> enumValues.get(random.nextInt(enumValues.size()));
        }

        Supplier<Object> generator = typeGenerators.get(type);

        if (generator == null) {
            throw new IllegalArgumentException("Unsupported field type: " + type);
        }

        return generator;
    }

    /**
     * Internal record for parsed schema
     */
    private record ParsedSchema(String type, List<?> enumValues) {}
}