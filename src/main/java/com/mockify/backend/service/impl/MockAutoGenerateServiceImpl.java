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

            Map.entry("email", () -> faker().internet().emailAddress()),

            Map.entry("date", () -> LocalDate.now().toString()),
            Map.entry("datetime", () -> Instant.now().toString()),

            Map.entry("uuid", () -> UUID.randomUUID().toString()),
            Map.entry("url", () -> faker().internet().url()),

            Map.entry("null", () -> null),

            Map.entry("array", () -> List.of(
                    faker().lorem().word(),
                    faker().number().randomDigit()
            )),

            Map.entry("object", () -> Map.of(
                    "value", faker().lorem().word()
            )),

            Map.entry("json", () -> Map.of(
                    "key", faker().lorem().word()
            ))
    );

    @Override
    public Map<String, Object> generateRecord(Map<String, Object> schemaJson) {

        if (schemaJson == null) {
            throw new IllegalArgumentException("Schema cannot be null");
        }

        Map<String, Object> record = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : schemaJson.entrySet()) {

            String field = entry.getKey();
            Object schemaDef = entry.getValue();

            ParsedSchema parsed = parseSchema(field, schemaDef);

            Supplier<Object> generator =
                    resolveGenerator(field, parsed);

            record.put(field, generator.get());
        }

        return record;
    }

    /**
     * Explicit schema type always wins.
     * Field-name generators are only used for generic string fields.
     */
    private Supplier<Object> resolveGenerator(
            String field,
            ParsedSchema parsed
    ) {

        Supplier<Object> typeGenerator =
                resolveTypeGenerator(
                        parsed.type(),
                        parsed.enumValues()
                );

        if (!"string".equals(parsed.type())) {
            return typeGenerator;
        }

        return resolveFieldGenerator(field)
                .orElse(typeGenerator);
    }

    /**
     * Parse schema definition safely
     */
    private ParsedSchema parseSchema(
            String field,
            Object schemaDef
    ) {

        if (schemaDef instanceof String s) {
            return new ParsedSchema(
                    s.toLowerCase(),
                    null
            );
        }

        if (schemaDef instanceof Map<?, ?> defMap) {

            Object typeObj = defMap.get("type");

            if (!(typeObj instanceof String typeStr)) {
                throw new IllegalArgumentException(
                        "Missing or invalid 'type' for field: "
                                + field
                );
            }

            List<?> enumValues = null;

            Object valuesObj = defMap.get("values");

            if (valuesObj instanceof List<?>) {
                enumValues = (List<?>) valuesObj;
            }

            return new ParsedSchema(
                    typeStr.toLowerCase(),
                    enumValues
            );
        }

        throw new IllegalArgumentException(
                "Invalid schema format for field: "
                        + field
        );
    }

    /**
     * Safer field-name matching.
     * Only applied for STRING schema types.
     */
    private Optional<Supplier<Object>> resolveFieldGenerator(
            String field
    ) {

        String f = field.toLowerCase();

        switch (f) {
            case "id":
                return Optional.ofNullable(fieldGenerators.get("id"));

            case "name":
            case "firstname":
            case "lastname":
                return Optional.ofNullable(fieldGenerators.get("name"));

            case "username":
                return Optional.ofNullable(fieldGenerators.get("username"));

            case "email":
                return Optional.ofNullable(fieldGenerators.get("email"));

            case "phone":
                return Optional.ofNullable(fieldGenerators.get("phone"));

            case "city":
                return Optional.ofNullable(fieldGenerators.get("city"));

            case "state":
                return Optional.ofNullable(fieldGenerators.get("state"));

            case "country":
                return Optional.ofNullable(fieldGenerators.get("country"));

            case "zipcode":
                return Optional.ofNullable(fieldGenerators.get("zipCode"));

            case "company":
                return Optional.ofNullable(fieldGenerators.get("company"));

            case "title":
                return Optional.ofNullable(fieldGenerators.get("title"));

            case "createdat":
                return Optional.ofNullable(fieldGenerators.get("createdAt"));

            case "updatedat":
                return Optional.ofNullable(fieldGenerators.get("updatedAt"));

            case "uuid":
                return Optional.ofNullable(fieldGenerators.get("uuid"));

            case "url":
                return Optional.ofNullable(fieldGenerators.get("url"));

            default:
                return Optional.empty();
        }
    }

    /**
     * Type-based generator with ENUM support
     */
    private Supplier<Object> resolveTypeGenerator(
            String type,
            List<?> enumValues
    ) {

        if ("enum".equals(type)) {

            if (enumValues == null || enumValues.isEmpty()) {
                throw new IllegalArgumentException(
                        "ENUM type requires non-empty values list"
                );
            }

            return () -> enumValues.get(
                    ThreadLocalRandom.current()
                            .nextInt(enumValues.size())
            );
        }

        Supplier<Object> generator =
                typeGenerators.get(type);

        if (generator == null) {
            throw new IllegalArgumentException(
                    "Unsupported field type: " + type
            );
        }

        return generator;
    }

    /**
     * Internal parsed schema holder
     */
    private record ParsedSchema(
            String type,
            List<?> enumValues
    ) {
    }
}
