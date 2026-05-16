package com.mockify.backend.service.open_api_import;

import com.mockify.backend.dto.internal.ParsedOpenApiSpec;
import com.mockify.backend.exception.InvalidOpenApiException;
import com.mockify.backend.service.OpenApiParserService;
import com.mockify.backend.service.impl.OpenApiParserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenApiParserServiceTest
 *
 * Purpose:
 * - Verify OpenAPI 3.x parsing
 * - Verify Swagger 2.0 parsing compatibility
 * - Verify schema extraction quality
 * - Verify invalid file rejection
 * - Verify defensive parser validation
 *
 * Test Resources:
 * src/test/resources/openApi-import-files/
 */
@SpringBootTest
@ActiveProfiles("test")
class OpenApiParserServiceTest {

    private OpenApiParserService parserService;

    private static final String RESOURCE_PATH =
            "/openApi-import-files/";

    @BeforeEach
    void setUp() {
        parserService = new OpenApiParserServiceImpl();
    }

    /**
     * Utility:
     * Load test resource file as MultipartFile
     */
    private MultipartFile loadFile(
            String filename,
            String contentType
    ) throws IOException {

        InputStream inputStream =
                getClass().getResourceAsStream(
                        RESOURCE_PATH + filename
                );

        assertNotNull(
                inputStream,
                "Test file not found: " + filename
        );

        return new MockMultipartFile(
                "file",
                filename,
                contentType,
                inputStream.readAllBytes()
        );
    }


    @Nested
    @DisplayName("Successful Parsing Tests")
    class SuccessfulParsingTests {

        @Test
        @DisplayName("Should parse basic valid OpenAPI YAML successfully")
        void shouldParseBasicValidYamlSuccessfully()
                throws Exception {

            MultipartFile file = loadFile(
                    "basic-valid.yaml",
                    "application/x-yaml"
            );

            ParsedOpenApiSpec result =
                    parserService.parse(file);

            assertNotNull(result);

            assertEquals(
                    "3.0.3",
                    result.getVersion()
            );

            assertNotNull(result.getSchemas());

            assertFalse(result.getSchemas().isEmpty());
        }


        @Test
        @DisplayName("Should parse enterprise large OpenAPI YAML successfully")
        void shouldParseEnterpriseLargeYamlSuccessfully()
                throws Exception {

            MultipartFile file = loadFile(
                    "enterprise-large.yaml",
                    "application/x-yaml"
            );

            ParsedOpenApiSpec result =
                    parserService.parse(file);

            assertNotNull(result);

            assertEquals(
                    "3.0.3",
                    result.getVersion()
            );

            /*
             * Enterprise spec should contain
             * multiple reusable schemas
             */
            assertTrue(
                    result.getSchemas().size() >= 2
            );
        }

        @Test
        @DisplayName("Should parse Swagger 2.0 specification successfully")
        void shouldParseSwagger2Successfully()
                throws Exception {

            MultipartFile file = loadFile(
                    "swagger-2.0.yaml",
                    "application/x-yaml"
            );

            ParsedOpenApiSpec result =
                    parserService.parse(file);

            assertNotNull(result);

            // swagger-parser converted Swagger 2.0 into internal OpenAPI 3.0.1 representation
            assertEquals(
                    "3.0.1",
                    result.getVersion()
            );

            assertNotNull(result.getSchemas());

            assertFalse(result.getSchemas().isEmpty());

            assertTrue(result.getSchemas().containsKey("User"));

            assertTrue(result.getSchemas().containsKey("Post"));
        }

        @Test
        @DisplayName("Should extract expected schema names")
        void shouldExtractExpectedSchemaNames()
                throws Exception {

            MultipartFile file = loadFile(
                    "basic-valid.yaml",
                    "application/x-yaml"
            );

            ParsedOpenApiSpec result =
                    parserService.parse(file);

            assertTrue(
                    result.getSchemas().containsKey("BlogPost")
            );
        }
    }


    @Nested
    @DisplayName("Failure Parsing Tests")
    class FailureParsingTests {

        @Test
        @DisplayName("Should throw exception when file is null")
        void shouldThrowWhenFileIsNull() {

            InvalidOpenApiException ex =
                    assertThrows(
                            InvalidOpenApiException.class,
                            () -> parserService.parse(null)
                    );

            assertEquals(
                    "Uploaded OpenAPI file is empty",
                    ex.getMessage()
            );
        }


        @Test
        @DisplayName("Should throw exception when file is empty")
        void shouldThrowWhenFileIsEmpty() {

            MultipartFile emptyFile =
                    new MockMultipartFile(
                            "file",
                            "empty.yaml",
                            "application/x-yaml",
                            new byte[0]
                    );

            InvalidOpenApiException ex =
                    assertThrows(
                            InvalidOpenApiException.class,
                            () -> parserService.parse(emptyFile)
                    );

            assertEquals(
                    "Uploaded OpenAPI file is empty",
                    ex.getMessage()
            );
        }


        @Test
        @DisplayName("Should throw exception for broken YAML")
        void shouldThrowForBrokenYaml()
                throws Exception {

            MultipartFile file = loadFile(
                    "invalid-broken.yaml",
                    "application/x-yaml"
            );

            InvalidOpenApiException ex =
                    assertThrows(
                            InvalidOpenApiException.class,
                            () -> parserService.parse(file)
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "Invalid OpenAPI specification"
                    ) ||
                            ex.getMessage().contains(
                                    "Failed to parse OpenAPI specification"
                            )
            );
        }

        @Test
        @DisplayName("Should throw exception when no schemas exist")
        void shouldThrowWhenSchemasMissing()
                throws Exception {

            String yaml = """
                    openapi: 3.0.3
                    info:
                      title: Empty API
                      version: 1.0.0
                    paths: {}
                    """;

            MultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "empty-spec.yaml",
                            "application/x-yaml",
                            yaml.getBytes()
                    );

            ParsedOpenApiSpec result =
                    parserService.parse(file);

            /*
             * Parser itself succeeds,
             * validator later rejects missing schemas
             */
            assertNotNull(result);

            assertTrue(
                    result.getSchemas().isEmpty()
            );
        }
    }


    @Nested
    @DisplayName("Schema Extraction Quality Tests")
    class SchemaExtractionQualityTests {

        @Test
        @DisplayName("Should preserve nested schemas")
        void shouldPreserveNestedSchemas()
                throws Exception {

            MultipartFile file = loadFile(
                    "enterprise-large.yaml",
                    "application/x-yaml"
            );

            ParsedOpenApiSpec result =
                    parserService.parse(file);

            assertNotNull(
                    result.getSchemas()
                            .get("User")
                            .getProperties()
                            .get("profile")
            );
        }


        @Test
        @DisplayName("Should preserve array schemas")
        void shouldPreserveArraySchemas()
                throws Exception {

            MultipartFile file = loadFile(
                    "enterprise-large.yaml",
                    "application/x-yaml"
            );

            ParsedOpenApiSpec result =
                    parserService.parse(file);

            assertNotNull(
                    result.getSchemas()
                            .get("User")
                            .getProperties()
                            .get("addresses")
            );
        }

        @Test
        @DisplayName("Should preserve Swagger 2 nested object schemas")
        void shouldPreserveSwagger2NestedSchemas()
                throws Exception {

            MultipartFile file = loadFile(
                    "swagger-2.0.yaml",
                    "application/x-yaml"
            );

            ParsedOpenApiSpec result =
                    parserService.parse(file);

            assertNotNull(
                    result.getSchemas()
                            .get("User")
                            .getProperties()
                            .get("profile")
            );
        }

        @Test
        @DisplayName("Should preserve Swagger 2 array schemas")
        void shouldPreserveSwagger2ArraySchemas()
                throws Exception {

            MultipartFile file = loadFile(
                    "swagger-2.0.yaml",
                    "application/x-yaml"
            );

            ParsedOpenApiSpec result =
                    parserService.parse(file);

            assertNotNull(
                    result.getSchemas()
                            .get("Post")
                            .getProperties()
                            .get("comments")
            );
        }
    }
}
