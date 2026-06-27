package com.mockify.backend.service.open_api_import;

import com.mockify.backend.service.SchemaExtractorService;
import com.mockify.backend.service.impl.SchemaExtractorServiceImpl;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
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
 * SchemaExtractorServiceTest
 *
 * Purpose:
 * - Verify OpenAPI → Mockify schema conversion
 * - Validate primitive type mapping
 * - Validate format conversions
 * - Validate nested objects
 * - Validate arrays
 * - Validate refs
 */

@SpringBootTest
@ActiveProfiles("test")
class SchemaExtractorServiceTest {

    private SchemaExtractorService extractorService;

    @BeforeEach
    void setUp() {
        extractorService = new SchemaExtractorServiceImpl();
    }

    /**
     * Helper:
     * Build schema with properties
     */
    private ObjectSchema buildSchema(
            Map<String, Schema> properties
    ) {

        ObjectSchema schema = new ObjectSchema();
        schema.setProperties(properties);

        return schema;
    }


    @Nested
    @DisplayName("Primitive Type Extraction Tests")
    class PrimitiveTypeExtractionTests {

        @Test
        @DisplayName("Should extract primitive fields correctly")
        void shouldExtractPrimitiveFieldsCorrectly() {

            Map<String, Schema> properties =
                    new LinkedHashMap<>();

            properties.put("name", new StringSchema());
            properties.put("age", new IntegerSchema());
            properties.put("price", new NumberSchema());
            properties.put("active", new BooleanSchema());

            Map<String, Object> result =
                    extractorService.extractSchema(
                            "User",
                            buildSchema(properties)
                    );

            assertEquals("string", result.get("name"));
            assertEquals("number", result.get("age"));
            assertEquals("number", result.get("price"));
            assertEquals("boolean", result.get("active"));
        }
    }


    @Nested
    @DisplayName("String Format Mapping Tests")
    class StringFormatMappingTests {

        @Test
        @DisplayName("Should map UUID format correctly")
        void shouldMapUuidCorrectly() {

            Map<String, Schema> properties =
                    new LinkedHashMap<>();

            properties.put(
                    "id",
                    new StringSchema().format("uuid")
            );

            Map<String, Object> result =
                    extractorService.extractSchema(
                            "User",
                            buildSchema(properties)
                    );

            assertEquals(
                    "uuid",
                    result.get("id")
            );
        }


        @Test
        @DisplayName("Should map email format correctly")
        void shouldMapEmailCorrectly() {

            Map<String, Schema> properties =
                    new LinkedHashMap<>();

            properties.put(
                    "email",
                    new StringSchema().format("email")
            );

            Map<String, Object> result =
                    extractorService.extractSchema(
                            "User",
                            buildSchema(properties)
                    );

            assertEquals(
                    "email",
                    result.get("email")
            );
        }


        @Test
        @DisplayName("Should map datetime format correctly")
        void shouldMapDateTimeCorrectly() {

            Map<String, Schema> properties =
                    new LinkedHashMap<>();

            properties.put(
                    "createdAt",
                    new StringSchema().format("date-time")
            );

            Map<String, Object> result =
                    extractorService.extractSchema(
                            "User",
                            buildSchema(properties)
                    );

            assertEquals(
                    "datetime",
                    result.get("createdAt")
            );
        }


        @Test
        @DisplayName("Should map URL format correctly")
        void shouldMapUrlCorrectly() {

            Map<String, Schema> properties =
                    new LinkedHashMap<>();

            properties.put(
                    "avatarUrl",
                    new StringSchema().format("uri")
            );

            Map<String, Object> result =
                    extractorService.extractSchema(
                            "User",
                            buildSchema(properties)
                    );

            assertEquals(
                    "url",
                    result.get("avatarUrl")
            );
        }
    }


    @Nested
    @DisplayName("Nested Object Extraction Tests")
    class NestedObjectExtractionTests {

        @Test
        @DisplayName("Should extract nested object correctly")
        void shouldExtractNestedObjectCorrectly() {

            ObjectSchema profileSchema =
                    new ObjectSchema();

            Map<String, Schema> nestedProps =
                    new LinkedHashMap<>();

            nestedProps.put("age", new IntegerSchema());
            nestedProps.put("gender", new StringSchema());

            profileSchema.setProperties(nestedProps);

            Map<String, Schema> properties =
                    new LinkedHashMap<>();

            properties.put("profile", profileSchema);

            Map<String, Object> result =
                    extractorService.extractSchema(
                            "User",
                            buildSchema(properties)
                    );

            assertTrue(
                    result.get("profile") instanceof Map
            );

            Map<?, ?> profile =
                    (Map<?, ?>) result.get("profile");

            assertEquals(
                    "number",
                    profile.get("age")
            );

            assertEquals(
                    "string",
                    profile.get("gender")
            );
        }


        @Test
        @DisplayName("Should fallback empty object correctly")
        void shouldFallbackEmptyObjectCorrectly() {

            ObjectSchema emptyObject =
                    new ObjectSchema();

            Map<String, Schema> properties =
                    new LinkedHashMap<>();

            properties.put("metadata", emptyObject);

            Map<String, Object> result =
                    extractorService.extractSchema(
                            "User",
                            buildSchema(properties)
                    );

            assertTrue(
                    result.get("metadata") instanceof Map
            );

            Map<?, ?> metadata =
                    (Map<?, ?>) result.get("metadata");

            assertEquals(
                    "object",
                    metadata.get("type")
            );
        }
    }


    @Nested
    @DisplayName("Array Extraction Tests")
    class ArrayExtractionTests {

        @Test
        @DisplayName("Should extract primitive array correctly")
        void shouldExtractPrimitiveArrayCorrectly() {

            ArraySchema tags =
                    new ArraySchema();

            tags.setItems(new StringSchema());

            Map<String, Schema> properties =
                    new LinkedHashMap<>();

            properties.put("tags", tags);

            Map<String, Object> result =
                    extractorService.extractSchema(
                            "Product",
                            buildSchema(properties)
                    );

            assertTrue(
                    result.get("tags") instanceof Map
            );

            Map<?, ?> array =
                    (Map<?, ?>) result.get("tags");

            assertEquals(
                    "array",
                    array.get("type")
            );

            assertEquals(
                    "string",
                    array.get("items")
            );
        }


        @Test
        @DisplayName("Should extract object array correctly")
        void shouldExtractObjectArrayCorrectly() {

            ObjectSchema address =
                    new ObjectSchema();

            Map<String, Schema> addressProps =
                    new LinkedHashMap<>();

            addressProps.put("city", new StringSchema());
            addressProps.put("zipCode", new StringSchema());

            address.setProperties(addressProps);

            ArraySchema addresses =
                    new ArraySchema();

            addresses.setItems(address);

            Map<String, Schema> properties =
                    new LinkedHashMap<>();

            properties.put("addresses", addresses);

            Map<String, Object> result =
                    extractorService.extractSchema(
                            "User",
                            buildSchema(properties)
                    );

            Map<?, ?> array =
                    (Map<?, ?>) result.get("addresses");

            assertEquals(
                    "array",
                    array.get("type")
            );

            assertTrue(
                    array.get("items") instanceof Map
            );

            Map<?, ?> nestedItems =
                    (Map<?, ?>) array.get("items");

            assertEquals(
                    "string",
                    nestedItems.get("city")
            );
        }


        @Test
        @DisplayName("Should fallback missing array items to string")
        void shouldFallbackMissingArrayItemsToString() {

            ArraySchema array =
                    new ArraySchema();

            Map<String, Schema> properties =
                    new LinkedHashMap<>();

            properties.put("tags", array);

            Map<String, Object> result =
                    extractorService.extractSchema(
                            "Product",
                            buildSchema(properties)
                    );

            Map<?, ?> extracted =
                    (Map<?, ?>) result.get("tags");

            assertEquals(
                    "string",
                    extracted.get("items")
            );
        }
    }


    @Nested
    @DisplayName("Reference Extraction Tests")
    class ReferenceExtractionTests {

        @Test
        @DisplayName("Should treat $ref as object")
        void shouldTreatRefAsObject() {

            Schema<?> refSchema =
                    new Schema<>()
                            .$ref("#/components/schemas/User");

            Map<String, Schema> properties =
                    new LinkedHashMap<>();

            properties.put("author", refSchema);

            Map<String, Object> result =
                    extractorService.extractSchema(
                            "BlogPost",
                            buildSchema(properties)
                    );

            assertEquals(
                    "object",
                    result.get("author")
            );
        }
    }


    @Nested
    @DisplayName("Failure Tests")
    class FailureTests {

        @Test
        @DisplayName("Should reject null schema")
        void shouldRejectNullSchema() {

            IllegalArgumentException ex =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> extractorService.extractSchema(
                                    "User",
                                    null
                            )
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "contains no extractable properties"
                    )
            );
        }


        @Test
        @DisplayName("Should reject schema without properties")
        void shouldRejectSchemaWithoutProperties() {

            ObjectSchema empty =
                    new ObjectSchema();

            IllegalArgumentException ex =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> extractorService.extractSchema(
                                    "User",
                                    empty
                            )
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "contains no extractable properties"
                    )
            );
        }
    }
}
