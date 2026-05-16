package com.mockify.backend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MockAutoGenerateServiceImplTest {

    private MockAutoGenerateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MockAutoGenerateServiceImpl();
    }

    @Test
    void shouldGenerateRecordWithFieldGenerators() {

        Map<String, Object> schema = Map.of(
                "id", "number",
                "name", "string",
                "firstName", "string",
                "lastName", "string",
                "username", "string",
                "email", "string"
        );

        Map<String, Object> result = service.generateRecord(schema);

        assertNotNull(result);

        assertTrue(result.get("id") instanceof Number);

        assertAll(
                () -> assertNotNull(result.get("name")),
                () -> assertNotNull(result.get("firstName")),
                () -> assertNotNull(result.get("lastName")),
                () -> assertNotNull(result.get("username"))
        );

        assertTrue(
                result.get("email")
                        .toString()
                        .contains("@")
        );
    }

    @Test
    void shouldGenerateRecordWithExtendedFieldGenerators() {

        Map<String, Object> schema = Map.ofEntries(
                Map.entry("phone", "string"),
                Map.entry("city", "string"),
                Map.entry("state", "string"),
                Map.entry("country", "string"),
                Map.entry("zipCode", "string"),
                Map.entry("company", "string"),
                Map.entry("title", "string"),
                Map.entry("createdAt", "string"),
                Map.entry("updatedAt", "string"),
                Map.entry("uuid", "string"),
                Map.entry("url", "string")
        );

        Map<String, Object> result = service.generateRecord(schema);

        assertNotNull(result);

        assertAll(
                () -> assertNotNull(result.get("phone")),
                () -> assertNotNull(result.get("city")),
                () -> assertNotNull(result.get("state")),
                () -> assertNotNull(result.get("country")),
                () -> assertNotNull(result.get("zipCode")),
                () -> assertNotNull(result.get("company")),
                () -> assertNotNull(result.get("title")),
                () -> assertNotNull(result.get("createdAt")),
                () -> assertNotNull(result.get("updatedAt"))
        );

        assertDoesNotThrow(() ->
                UUID.fromString(result.get("uuid").toString())
        );

        String url = result.get("url").toString();

        assertAll(
                () -> assertNotNull(url),
                () -> assertFalse(url.isBlank()),
                () -> assertTrue(url.contains("."))
        );
    }

    @Test
    void shouldGenerateRecordWithTypeGenerators() {

        Map<String, Object> schema = Map.of(
                "description", "string",
                "age", "number",
                "active", "boolean",
                "birthDate", "date",
                "eventTime", "date-time",
                "tags", "array",
                "metadata", "object"
        );

        Map<String, Object> result = service.generateRecord(schema);

        assertNotNull(result);

        assertAll(
                () -> assertTrue(result.get("description") instanceof String),
                () -> assertTrue(result.get("age") instanceof Number),
                () -> assertTrue(result.get("active") instanceof Boolean),
                () -> assertTrue(result.get("birthDate") instanceof String),
                () -> assertTrue(result.get("eventTime") instanceof String),
                () -> assertTrue(result.get("tags") instanceof List),
                () -> assertTrue(result.get("metadata") instanceof Map)
        );
    }

    @Test
    void shouldGenerateEnumValue() {

        List<String> allowedStatuses = List.of(
                "ACTIVE",
                "INACTIVE",
                "PENDING"
        );

        Map<String, Object> schema = Map.of(
                "status", Map.of(
                        "type", "enum",
                        "values", allowedStatuses
                )
        );

        Map<String, Object> result = service.generateRecord(schema);

        assertTrue(
                allowedStatuses.contains(result.get("status"))
        );
    }

    @Test
    void shouldThrowExceptionForUnsupportedType() {

        Map<String, Object> schema = Map.of(
                "salary", "decimal"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generateRecord(schema)
        );

        assertEquals(
                "Unsupported field type: decimal",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionForInvalidSchemaFormat() {

        Map<String, Object> schema = Map.of(
                "field", 123
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generateRecord(schema)
        );

        assertEquals(
                "Invalid schema format for field: field",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenTypeMissingInMapSchema() {

        Map<String, Object> schema = Map.of(
                "status", Map.of(
                        "values", List.of("A", "B")
                )
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generateRecord(schema)
        );

        assertEquals(
                "Missing or invalid 'type' for field: status",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionForEmptyEnumValues() {

        Map<String, Object> schema = Map.of(
                "status", Map.of(
                        "type", "enum",
                        "values", List.of()
                )
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generateRecord(schema)
        );

        assertEquals(
                "ENUM type requires non-empty values list",
                exception.getMessage()
        );
    }
}