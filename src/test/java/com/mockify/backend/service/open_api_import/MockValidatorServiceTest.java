package com.mockify.backend.service.open_api_import;

import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.service.MockValidatorService;
import com.mockify.backend.service.impl.MockValidatorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MockValidatorServiceTest
 *
 * Purpose:
 * - Validate schema definitions
 * - Validate records against schemas
 * - Verify nested objects
 * - Verify arrays
 * - Verify enum handling
 * - Verify URL / UUID / Email / Datetime
 */
@SpringBootTest
@ActiveProfiles("test")
class MockValidatorServiceTest {

    private MockValidatorService validatorService;

    @BeforeEach
    void setUp() {
        validatorService = new MockValidatorServiceImpl();
    }

    /**
     * Helper:
     * Build valid primitive schema
     */
    private Map<String, Object> buildBasicSchema() {

        Map<String, Object> schema =
                new LinkedHashMap<>();

        schema.put("id", "uuid");
        schema.put("name", "string");
        schema.put("email", "email");
        schema.put("active", "boolean");
        schema.put("createdAt", "datetime");

        return schema;
    }


    @Nested
    @DisplayName("Schema Definition Validation Tests")
    class SchemaDefinitionValidationTests {

        @Test
        @DisplayName("Should validate primitive schema")
        void shouldValidatePrimitiveSchema() {

            Map<String, Object> schema =
                    buildBasicSchema();

            assertDoesNotThrow(
                    () -> validatorService.validateSchemaDefinition(schema)
            );
        }


        @Test
        @DisplayName("Should validate nested object schema")
        void shouldValidateNestedObjectSchema() {

            Map<String, Object> profile =
                    new LinkedHashMap<>();

            profile.put("age", "number");
            profile.put("gender", "string");

            Map<String, Object> schema =
                    new LinkedHashMap<>();

            schema.put("profile", profile);

            assertDoesNotThrow(
                    () -> validatorService.validateSchemaDefinition(schema)
            );
        }


        @Test
        @DisplayName("Should validate array schema")
        void shouldValidateArraySchema() {

            Map<String, Object> tags =
                    new LinkedHashMap<>();

            tags.put("type", "array");
            tags.put("items", "string");

            Map<String, Object> schema =
                    new LinkedHashMap<>();

            schema.put("tags", tags);

            assertDoesNotThrow(
                    () -> validatorService.validateSchemaDefinition(schema)
            );
        }


        @Test
        @DisplayName("Should validate enum schema")
        void shouldValidateEnumSchema() {

            Map<String, Object> status =
                    new LinkedHashMap<>();

            status.put("type", "enum");
            status.put(
                    "values",
                    List.of("PENDING", "PAID", "FAILED")
            );

            Map<String, Object> schema =
                    new LinkedHashMap<>();

            schema.put("status", status);

            assertDoesNotThrow(
                    () -> validatorService.validateSchemaDefinition(schema)
            );
        }


        @Test
        @DisplayName("Should reject empty schema")
        void shouldRejectEmptySchema() {

            BadRequestException ex =
                    assertThrows(
                            BadRequestException.class,
                            () -> validatorService.validateSchemaDefinition(
                                    new LinkedHashMap<>()
                            )
                    );

            assertEquals(
                    "Schema JSON cannot be empty",
                    ex.getMessage()
            );
        }


        @Test
        @DisplayName("Should reject invalid type")
        void shouldRejectInvalidType() {

            Map<String, Object> schema =
                    new LinkedHashMap<>();

            schema.put("avatar", "file");

            BadRequestException ex =
                    assertThrows(
                            BadRequestException.class,
                            () -> validatorService.validateSchemaDefinition(schema)
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "Invalid type"
                    )
            );
        }


        @Test
        @DisplayName("Should reject enum without values")
        void shouldRejectEnumWithoutValues() {

            Map<String, Object> status =
                    new LinkedHashMap<>();

            status.put("type", "enum");

            Map<String, Object> schema =
                    new LinkedHashMap<>();

            schema.put("status", status);

            BadRequestException ex =
                    assertThrows(
                            BadRequestException.class,
                            () -> validatorService.validateSchemaDefinition(schema)
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "must define non-empty values"
                    )
            );
        }


        @Test
        @DisplayName("Should reject array without items")
        void shouldRejectArrayWithoutItems() {

            Map<String, Object> tags =
                    new LinkedHashMap<>();

            tags.put("type", "array");

            Map<String, Object> schema =
                    new LinkedHashMap<>();

            schema.put("tags", tags);

            BadRequestException ex =
                    assertThrows(
                            BadRequestException.class,
                            () -> validatorService.validateSchemaDefinition(schema)
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "must define items"
                    )
            );
        }
    }


    @Nested
    @DisplayName("Record Validation Success Tests")
    class RecordValidationSuccessTests {

        @Test
        @DisplayName("Should validate valid primitive record")
        void shouldValidateValidPrimitiveRecord() {

            Map<String, Object> schema =
                    buildBasicSchema();

            Map<String, Object> record =
                    new LinkedHashMap<>();

            record.put(
                    "id",
                    UUID.randomUUID().toString()
            );

            record.put("name", "Sadab");
            record.put("email", "sadab@example.com");
            record.put("active", true);
            record.put(
                    "createdAt",
                    "2026-05-16T10:15:30+00:00"
            );

            assertDoesNotThrow(
                    () -> validatorService.validateRecordAgainstSchema(
                            schema,
                            record
                    )
            );
        }


        @Test
        @DisplayName("Should validate nested object record")
        void shouldValidateNestedObjectRecord() {

            Map<String, Object> profile =
                    new LinkedHashMap<>();

            profile.put("age", "number");
            profile.put("gender", "string");

            Map<String, Object> schema =
                    new LinkedHashMap<>();

            schema.put("profile", profile);

            Map<String, Object> profileRecord =
                    new LinkedHashMap<>();

            profileRecord.put("age", 25);
            profileRecord.put("gender", "Male");

            Map<String, Object> record =
                    new LinkedHashMap<>();

            record.put("profile", profileRecord);

            assertDoesNotThrow(
                    () -> validatorService.validateRecordAgainstSchema(
                            schema,
                            record
                    )
            );
        }


        @Test
        @DisplayName("Should validate URL field")
        void shouldValidateUrlField() {

            Map<String, Object> schema =
                    new LinkedHashMap<>();

            schema.put("website", "url");

            Map<String, Object> record =
                    new LinkedHashMap<>();

            record.put(
                    "website",
                    "https://mockify.com"
            );

            assertDoesNotThrow(
                    () -> validatorService.validateRecordAgainstSchema(
                            schema,
                            record
                    )
            );
        }


        @Test
        @DisplayName("Should validate enum record")
        void shouldValidateEnumRecord() {

            Map<String, Object> status =
                    new LinkedHashMap<>();

            status.put("type", "enum");
            status.put(
                    "values",
                    List.of("PENDING", "PAID")
            );

            Map<String, Object> schema =
                    new LinkedHashMap<>();

            schema.put("status", status);

            Map<String, Object> record =
                    new LinkedHashMap<>();

            record.put("status", "PAID");

            assertDoesNotThrow(
                    () -> validatorService.validateRecordAgainstSchema(
                            schema,
                            record
                    )
            );
        }
    }


    @Nested
    @DisplayName("Record Validation Failure Tests")
    class RecordValidationFailureTests {

        @Test
        @DisplayName("Should reject missing required field")
        void shouldRejectMissingField() {

            Map<String, Object> schema =
                    buildBasicSchema();

            Map<String, Object> record =
                    new LinkedHashMap<>();

            record.put("name", "Sadab");

            BadRequestException ex =
                    assertThrows(
                            BadRequestException.class,
                            () -> validatorService.validateRecordAgainstSchema(
                                    schema,
                                    record
                            )
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "Missing field"
                    )
            );
        }


        @Test
        @DisplayName("Should reject invalid email")
        void shouldRejectInvalidEmail() {

            Map<String, Object> schema =
                    new LinkedHashMap<>();

            schema.put("email", "email");

            Map<String, Object> record =
                    new LinkedHashMap<>();

            record.put("email", "invalid-email");

            BadRequestException ex =
                    assertThrows(
                            BadRequestException.class,
                            () -> validatorService.validateRecordAgainstSchema(
                                    schema,
                                    record
                            )
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "Invalid email format"
                    )
            );
        }


        @Test
        @DisplayName("Should reject invalid UUID")
        void shouldRejectInvalidUuid() {

            Map<String, Object> schema =
                    new LinkedHashMap<>();

            schema.put("id", "uuid");

            Map<String, Object> record =
                    new LinkedHashMap<>();

            record.put("id", "123");

            BadRequestException ex =
                    assertThrows(
                            BadRequestException.class,
                            () -> validatorService.validateRecordAgainstSchema(
                                    schema,
                                    record
                            )
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "Invalid UUID format"
                    )
            );
        }


        @Test
        @DisplayName("Should reject invalid datetime")
        void shouldRejectInvalidDatetime() {

            Map<String, Object> schema =
                    new LinkedHashMap<>();

            schema.put("createdAt", "datetime");

            Map<String, Object> record =
                    new LinkedHashMap<>();

            record.put("createdAt", "invalid-date");

            BadRequestException ex =
                    assertThrows(
                            BadRequestException.class,
                            () -> validatorService.validateRecordAgainstSchema(
                                    schema,
                                    record
                            )
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "Invalid datetime format"
                    )
            );
        }


        @Test
        @DisplayName("Should reject invalid URL")
        void shouldRejectInvalidUrl() {

            Map<String, Object> schema =
                    new LinkedHashMap<>();

            schema.put("website", "url");

            Map<String, Object> record =
                    new LinkedHashMap<>();

            record.put("website", "ht!tp://bad-url");

            BadRequestException ex =
                    assertThrows(
                            BadRequestException.class,
                            () -> validatorService.validateRecordAgainstSchema(
                                    schema,
                                    record
                            )
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "Invalid URL format"
                    )
            );
        }


        @Test
        @DisplayName("Should reject invalid enum value")
        void shouldRejectInvalidEnumValue() {

            Map<String, Object> status =
                    new LinkedHashMap<>();

            status.put("type", "enum");
            status.put(
                    "values",
                    List.of("PENDING", "PAID")
            );

            Map<String, Object> schema =
                    new LinkedHashMap<>();

            schema.put("status", status);

            Map<String, Object> record =
                    new LinkedHashMap<>();

            record.put("status", "FAILED");

            BadRequestException ex =
                    assertThrows(
                            BadRequestException.class,
                            () -> validatorService.validateRecordAgainstSchema(
                                    schema,
                                    record
                            )
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "Invalid enum value"
                    )
            );
        }


        @Test
        @DisplayName("Should reject extra fields")
        void shouldRejectExtraFields() {

            Map<String, Object> schema =
                    new LinkedHashMap<>();

            schema.put("name", "string");

            Map<String, Object> record =
                    new LinkedHashMap<>();

            record.put("name", "Sadab");
            record.put("extra", "not allowed");

            BadRequestException ex =
                    assertThrows(
                            BadRequestException.class,
                            () -> validatorService.validateRecordAgainstSchema(
                                    schema,
                                    record
                            )
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "not allowed"
                    )
            );
        }
    }
}
