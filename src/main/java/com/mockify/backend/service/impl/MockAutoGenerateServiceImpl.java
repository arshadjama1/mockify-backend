package com.mockify.backend.service.impl;

import com.github.javafaker.Faker;
import com.mockify.backend.service.MockAutoGenerateService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Supplier;

@Service
public class MockAutoGenerateServiceImpl implements MockAutoGenerateService {

    private final Faker faker = new Faker();

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
            String type = String.valueOf(entry.getValue()).toLowerCase();

            Supplier<Object> generator = resolveFieldGenerator(field)
                    .orElseGet(() -> resolveTypeGenerator(type));

            record.put(field, generator.get());
        }

        return record;
    }

    /**
     * Try to resolve generator based on field name (fuzzy matching)
     */
    private Optional<Supplier<Object>> resolveFieldGenerator(String field) {
        String f = field.toLowerCase();

        if (f.contains("email")) return Optional.of(fieldGenerators.get("email"));
        if (f.contains("username")) return Optional.of(fieldGenerators.get("username"));
        if (f.equals("id") || f.endsWith("id")) return Optional.of(fieldGenerators.get("id"));
        if (f.contains("name")) return Optional.of(fieldGenerators.get("name"));

        return Optional.empty();
    }

    /**
     * Resolve generator based on type or throw clear error
     */
    private Supplier<Object> resolveTypeGenerator(String type) {
        Supplier<Object> generator = typeGenerators.get(type);

        if (generator == null) {
            throw new IllegalArgumentException("Unsupported field type: " + type);
        }

        return generator;
    }
}