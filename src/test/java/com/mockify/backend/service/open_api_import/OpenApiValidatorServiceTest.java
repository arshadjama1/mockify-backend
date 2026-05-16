package com.mockify.backend.service.open_api_import;

import com.mockify.backend.dto.internal.ParsedOpenApiSpec;
import com.mockify.backend.exception.InvalidOpenApiException;
import com.mockify.backend.service.OpenApiValidatorService;
import com.mockify.backend.service.impl.OpenApiValidatorServiceImpl;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenApiValidatorServiceTest
 *
 * Purpose:
 * - Validate OpenAPI spec business rules
 * - Ensure schema structure correctness
 * - Catch invalid component definitions
 * - Ensure supported type enforcement
 */
@SpringBootTest
@ActiveProfiles("test")
class OpenApiValidatorServiceTest {

    private OpenApiValidatorService validatorService;

    @BeforeEach
    void setUp() {
        validatorService = new OpenApiValidatorServiceImpl();
    }

    /**
     * Helper:
     * Build valid ParsedOpenApiSpec quickly
     */
    private ParsedOpenApiSpec buildValidSpec() {

        Map<String, Schema> schemas = new LinkedHashMap<>();

        ObjectSchema userSchema = new ObjectSchema();

        Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put("id", new StringSchema().format("uuid"));
        properties.put("name", new StringSchema());
        properties.put("email", new StringSchema().format("email"));
        properties.put("active", new BooleanSchema());

        userSchema.setProperties(properties);

        schemas.put("User", userSchema);

        return new ParsedOpenApiSpec(
                "3.0.3",
                schemas
        );
    }


    @Nested
    @DisplayName("Successful Validation Tests")
    class SuccessfulValidationTests {

        @Test
        @DisplayName("Should validate correct OpenAPI 3.x spec")
        void shouldValidateCorrectSpec() {

            ParsedOpenApiSpec spec = buildValidSpec();

            assertDoesNotThrow(
                    () -> validatorService.validate(spec)
            );
        }


        @Test
        @DisplayName("Should validate nested object schema")
        void shouldValidateNestedObjectSchema() {

            ObjectSchema nestedObject = new ObjectSchema();

            Map<String, Schema> nestedProps = new LinkedHashMap<>();
            nestedProps.put("age", new IntegerSchema());
            nestedProps.put("gender", new StringSchema());

            nestedObject.setProperties(nestedProps);

            ObjectSchema userSchema = new ObjectSchema();

            Map<String, Schema> properties = new LinkedHashMap<>();
            properties.put("profile", nestedObject);

            userSchema.setProperties(properties);

            Map<String, Schema> schemas = new LinkedHashMap<>();
            schemas.put("User", userSchema);

            ParsedOpenApiSpec spec =
                    new ParsedOpenApiSpec("3.0.3", schemas);

            assertDoesNotThrow(
                    () -> validatorService.validate(spec)
            );
        }


        @Test
        @DisplayName("Should validate array property")
        void shouldValidateArrayProperty() {

            ArraySchema tags = new ArraySchema();
            tags.setItems(new StringSchema());

            ObjectSchema productSchema = new ObjectSchema();

            Map<String, Schema> properties = new LinkedHashMap<>();
            properties.put("tags", tags);

            productSchema.setProperties(properties);

            Map<String, Schema> schemas = new LinkedHashMap<>();
            schemas.put("Product", productSchema);

            ParsedOpenApiSpec spec =
                    new ParsedOpenApiSpec("3.0.3", schemas);

            assertDoesNotThrow(
                    () -> validatorService.validate(spec)
            );
        }
    }


    @Nested
    @DisplayName("Version Validation Failure Tests")
    class VersionValidationFailureTests {

        @Test
        @DisplayName("Should reject null spec")
        void shouldRejectNullSpec() {

            InvalidOpenApiException ex =
                    assertThrows(
                            InvalidOpenApiException.class,
                            () -> validatorService.validate(null)
                    );

            assertEquals(
                    "Parsed OpenAPI specification is null",
                    ex.getMessage()
            );
        }


        @Test
        @DisplayName("Should reject missing version")
        void shouldRejectMissingVersion() {

            ParsedOpenApiSpec spec =
                    new ParsedOpenApiSpec(
                            null,
                            new LinkedHashMap<>()
                    );

            InvalidOpenApiException ex =
                    assertThrows(
                            InvalidOpenApiException.class,
                            () -> validatorService.validate(spec)
                    );

            assertEquals(
                    "Missing OpenAPI version",
                    ex.getMessage()
            );
        }


        @Test
        @DisplayName("Should reject Swagger invalid versions")
        void shouldRejectSwagger() {

            ParsedOpenApiSpec spec =
                    new ParsedOpenApiSpec(
                            "1.0",
                            new LinkedHashMap<>()
                    );

            InvalidOpenApiException ex =
                    assertThrows(
                            InvalidOpenApiException.class,
                            () -> validatorService.validate(spec)
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "Unsupported OpenAPI version"
                    )
            );
        }
    }


    @Nested
    @DisplayName("Schema Structure Failure Tests")
    class SchemaStructureFailureTests {

        @Test
        @DisplayName("Should reject missing schemas")
        void shouldRejectMissingSchemas() {

            ParsedOpenApiSpec spec =
                    new ParsedOpenApiSpec(
                            "3.0.3",
                            new LinkedHashMap<>()
                    );

            InvalidOpenApiException ex =
                    assertThrows(
                            InvalidOpenApiException.class,
                            () -> validatorService.validate(spec)
                    );

            assertEquals(
                    "No reusable schemas found in OpenAPI components",
                    ex.getMessage()
            );
        }


        @Test
        @DisplayName("Should reject blank schema name")
        void shouldRejectBlankSchemaName() {

            Map<String, Schema> schemas =
                    new LinkedHashMap<>();

            ObjectSchema schema = new ObjectSchema();
            schema.setProperties(
                    Map.of("id", new StringSchema())
            );

            schemas.put("", schema);

            ParsedOpenApiSpec spec =
                    new ParsedOpenApiSpec("3.0.3", schemas);

            InvalidOpenApiException ex =
                    assertThrows(
                            InvalidOpenApiException.class,
                            () -> validatorService.validate(spec)
                    );

            assertEquals(
                    "Schema name cannot be empty",
                    ex.getMessage()
            );
        }


        @Test
        @DisplayName("Should reject schema with no properties")
        void shouldRejectSchemaWithNoProperties() {

            Map<String, Schema> schemas =
                    new LinkedHashMap<>();

            ObjectSchema emptySchema =
                    new ObjectSchema();

            schemas.put("User", emptySchema);

            ParsedOpenApiSpec spec =
                    new ParsedOpenApiSpec("3.0.3", schemas);

            InvalidOpenApiException ex =
                    assertThrows(
                            InvalidOpenApiException.class,
                            () -> validatorService.validate(spec)
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "contains no properties"
                    )
            );
        }
    }


    @Nested
    @DisplayName("Property Validation Failure Tests")
    class PropertyValidationFailureTests {

        @Test
        @DisplayName("Should reject unsupported property type")
        void shouldRejectUnsupportedPropertyType() {

            Schema unsupported =
                    new Schema<>().type("abc");

            ObjectSchema schema =
                    new ObjectSchema();

            Map<String, Schema> properties =
                    new LinkedHashMap<>();

            properties.put("avatar", unsupported);

            schema.setProperties(properties);

            Map<String, Schema> schemas =
                    new LinkedHashMap<>();

            schemas.put("User", schema);

            ParsedOpenApiSpec spec =
                    new ParsedOpenApiSpec("3.0.3", schemas);

            InvalidOpenApiException ex =
                    assertThrows(
                            InvalidOpenApiException.class,
                            () -> validatorService.validate(spec)
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "Unsupported property type"
                    )
            );
        }


        @Test
        @DisplayName("Should reject property missing type")
        void shouldRejectPropertyMissingType() {

            Schema<?> missingType =
                    new Schema<>();

            ObjectSchema schema =
                    new ObjectSchema();

            Map<String, Schema> properties =
                    new LinkedHashMap<>();

            properties.put("profile", missingType);

            schema.setProperties(properties);

            Map<String, Schema> schemas =
                    new LinkedHashMap<>();

            schemas.put("User", schema);

            ParsedOpenApiSpec spec =
                    new ParsedOpenApiSpec("3.0.3", schemas);

            InvalidOpenApiException ex =
                    assertThrows(
                            InvalidOpenApiException.class,
                            () -> validatorService.validate(spec)
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "missing type"
                    )
            );
        }


        @Test
        @DisplayName("Should reject array without items")
        void shouldRejectArrayWithoutItems() {

            ArraySchema invalidArray =
                    new ArraySchema();

            ObjectSchema schema =
                    new ObjectSchema();

            Map<String, Schema> properties =
                    new LinkedHashMap<>();

            properties.put("tags", invalidArray);

            schema.setProperties(properties);

            Map<String, Schema> schemas =
                    new LinkedHashMap<>();

            schemas.put("Product", schema);

            ParsedOpenApiSpec spec =
                    new ParsedOpenApiSpec("3.0.3", schemas);

            InvalidOpenApiException ex =
                    assertThrows(
                            InvalidOpenApiException.class,
                            () -> validatorService.validate(spec)
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "missing item definition"
                    )
            );
        }


        @Test
        @DisplayName("Should allow $ref property")
        void shouldAllowRefProperty() {

            Schema<?> refSchema =
                    new Schema<>().$ref(
                            "#/components/schemas/User"
                    );

            ObjectSchema schema =
                    new ObjectSchema();

            Map<String, Schema> properties =
                    new LinkedHashMap<>();

            properties.put("author", refSchema);

            schema.setProperties(properties);

            Map<String, Schema> schemas =
                    new LinkedHashMap<>();

            schemas.put("BlogPost", schema);

            ParsedOpenApiSpec spec =
                    new ParsedOpenApiSpec("3.0.3", schemas);

            assertDoesNotThrow(
                    () -> validatorService.validate(spec)
            );
        }
    }
}
