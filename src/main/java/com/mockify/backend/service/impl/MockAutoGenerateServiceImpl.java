package com.mockify.backend.service.impl;

import com.github.javafaker.Faker;
import com.mockify.backend.service.MockAutoGenerateService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Service
public class MockAutoGenerateServiceImpl implements MockAutoGenerateService {

    private static final ThreadLocal<Faker> FAKER =
            ThreadLocal.withInitial(Faker::new);

    private Faker faker() {
        return FAKER.get();
    }

    // Exact field generators
    private final Map<String, Supplier<Object>> fieldGenerators = Map.ofEntries(
            Map.entry("id", () -> faker().number().numberBetween(1, 100000)),
            Map.entry("name", () -> faker().name().fullName()),
            Map.entry("firstName", () -> faker().name().firstName()),
            Map.entry("lastName", () -> faker().name().lastName()),
            Map.entry("username", () -> faker().name().username()),
            Map.entry("email", () -> faker().internet().emailAddress()),
            Map.entry("phone", () -> faker().phoneNumber().cellPhone()),
            Map.entry("city", () -> faker().address().city()),
            Map.entry("state", () -> faker().address().state()),
            Map.entry("country", () -> faker().address().country()),
            Map.entry("zipCode", () -> faker().address().zipCode()),
            Map.entry("company", () -> faker().company().name()),
            Map.entry("title", () -> faker().job().title()),
            Map.entry("createdAt", () -> Instant.now().toString()),
            Map.entry("updatedAt", () -> Instant.now().toString()),
            Map.entry("uuid", () -> UUID.randomUUID().toString()),
            Map.entry("url", () -> faker().internet().url())
    );

    // Type-based generators
    private final Map<String, Supplier<Object>> typeGenerators = Map.ofEntries(
            Map.entry("string", () -> faker().lorem().word()),
            Map.entry("number", () -> faker().number().numberBetween(1, 1000)),
            Map.entry("boolean", () -> faker().bool().bool()),
            Map.entry("date", () -> LocalDate.now().toString()),
            Map.entry("date-time", () -> Instant.now().toString()),
            Map.entry("uuid", () -> UUID.randomUUID().toString()),
            Map.entry("url", () -> faker().internet().url()),
            Map.entry("array", () -> List.of(
                    faker().lorem().word(),
                    faker().number().randomDigit()
            )),
            Map.entry("object", () -> Map.of(
                    "value", faker().lorem().word()
            ))
    );

    @Override
    public Map<String, Object> generateRecord(Map<String, Object> schemaJson) {

        Map<String, Object> record = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : schemaJson.entrySet()) {

            String field = entry.getKey();
            Object schemaDef = entry.getValue();

            ParsedSchema parsed = parseSchema(field, schemaDef);

            Supplier<Object> generator = resolveFieldGenerator(field)
                    .orElseGet(() ->
                            resolveTypeGenerator(
                                    parsed.type(),
                                    parsed.enumValues()
                            )
                    );

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

        if (f.contains("email"))
            return Optional.ofNullable(fieldGenerators.get("email"));

        if (f.contains("username"))
            return Optional.ofNullable(fieldGenerators.get("username"));

        if (f.contains("uuid"))
            return Optional.ofNullable(fieldGenerators.get("uuid"));

        if (f.contains("url"))
            return Optional.ofNullable(fieldGenerators.get("url"));

        if (f.contains("phone"))
            return Optional.ofNullable(fieldGenerators.get("phone"));

        if (f.equals("id") || f.endsWith("id"))
            return Optional.ofNullable(fieldGenerators.get("id"));

        if (f.contains("name"))
            return Optional.ofNullable(fieldGenerators.get("name"));

        if (f.contains("city"))
            return Optional.ofNullable(fieldGenerators.get("city"));

        if (f.contains("state"))
            return Optional.ofNullable(fieldGenerators.get("state"));

        if (f.contains("country"))
            return Optional.ofNullable(fieldGenerators.get("country"));

        if (f.contains("zipcode"))
            return Optional.ofNullable(fieldGenerators.get("zipCode"));

        if (f.contains("company"))
            return Optional.ofNullable(fieldGenerators.get("company"));

        if (f.contains("title"))
            return Optional.ofNullable(fieldGenerators.get("title"));

        if (f.contains("createdat"))
            return Optional.ofNullable(fieldGenerators.get("createdAt"));

        if (f.contains("updatedat"))
            return Optional.ofNullable(fieldGenerators.get("updatedAt"));

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
                throw new IllegalArgumentException(
                        "ENUM type requires non-empty values list"
                );
            }

            return () -> enumValues.get(
                    ThreadLocalRandom.current().nextInt(enumValues.size())
            );
        }

        Supplier<Object> generator = typeGenerators.get(type);

        if (generator == null) {
            throw new IllegalArgumentException(
                    "Unsupported field type: " + type
            );
        }

        return generator;
    }

    /**
     * Internal record for parsed schema
     */
    private record ParsedSchema(String type, List<?> enumValues) {}
}