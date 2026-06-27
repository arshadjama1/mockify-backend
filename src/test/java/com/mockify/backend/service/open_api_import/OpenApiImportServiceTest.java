package com.mockify.backend.service.open_api_import;

import com.mockify.backend.dto.internal.ParsedOpenApiSpec;
import com.mockify.backend.dto.request.schema.CreateMockSchemaRequest;
import com.mockify.backend.dto.response.imports.OpenApiImportResponse;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.exception.InvalidOpenApiException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.model.Project;
import com.mockify.backend.repository.ProjectRepository;
import com.mockify.backend.service.*;
import com.mockify.backend.service.impl.OpenApiImportServiceImpl;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OpenApiImportServiceTest
 *
 * Purpose:
 * - Validate orchestration layer
 * - Validate file rules
 * - Validate parser + validator + extractor integration
 * - Validate partial import behavior
 * - Validate skipped schemas
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class OpenApiImportServiceTest {

    private OpenApiParserService parserService;
    private OpenApiValidatorService validatorService;
    private SchemaExtractorService extractorService;
    private MockSchemaService mockSchemaService;
    private ProjectRepository projectRepository;

    private OpenApiImportService importService;

    private UUID userId;
    private UUID projectId;
    private Project project;

    @BeforeEach
    void setUp() {

        parserService = mock(OpenApiParserService.class);
        validatorService = mock(OpenApiValidatorService.class);
        extractorService = mock(SchemaExtractorService.class);
        mockSchemaService = mock(MockSchemaService.class);
        projectRepository = mock(ProjectRepository.class);

        importService = new OpenApiImportServiceImpl(
                parserService,
                validatorService,
                extractorService,
                mockSchemaService,
                projectRepository
        );

        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        project = new Project();
        project.setId(projectId);
    }

    /**
     * Helper:
     * Build valid multipart YAML file
     */
    private MultipartFile buildValidFile() {

        return new MockMultipartFile(
                "file",
                "openapi.yaml",
                "application/x-yaml",
                "openapi: 3.0.3".getBytes()
        );
    }

    /**
     * Helper:
     * Build parsed spec
     */
    private ParsedOpenApiSpec buildParsedSpec() {

        Map<String, Schema> schemas =
                new LinkedHashMap<>();

        ObjectSchema userSchema =
                new ObjectSchema();

        userSchema.setProperties(
                Map.of(
                        "id",
                        new StringSchema()
                )
        );

        ObjectSchema productSchema =
                new ObjectSchema();

        productSchema.setProperties(
                Map.of(
                        "name",
                        new StringSchema()
                )
        );

        schemas.put("User", userSchema);
        schemas.put("Product", productSchema);

        return new ParsedOpenApiSpec(
                "3.0.3",
                schemas
        );
    }


    @Nested
    @DisplayName("Successful Import Tests")
    class SuccessfulImportTests {

        @Test
        @DisplayName("Should import all schemas successfully")
        void shouldImportAllSchemasSuccessfully() {

            MultipartFile file = buildValidFile();
            ParsedOpenApiSpec parsedSpec = buildParsedSpec();

            when(projectRepository.findById(projectId))
                    .thenReturn(Optional.of(project));

            when(parserService.parse(file))
                    .thenReturn(parsedSpec);

            when(extractorService.extractSchema(anyString(), any()))
                    .thenAnswer(invocation -> {
                        String name = invocation.getArgument(0);

                        return Map.of(
                                "field",
                                "string"
                        );
                    });

            when(mockSchemaService.createSchema(
                    eq(userId),
                    eq(projectId),
                    any(CreateMockSchemaRequest.class)
            )).thenAnswer(invocation -> {

                CreateMockSchemaRequest req =
                        invocation.getArgument(2);

                MockSchemaResponse response =
                        new MockSchemaResponse();

                response.setName(req.getName());

                return response;
            });

            OpenApiImportResponse response =
                    importService.importOpenApi(
                            userId,
                            projectId,
                            file
                    );

            assertNotNull(response);

            assertEquals(
                    2,
                    response.getTotalImported()
            );

            assertEquals(
                    0,
                    response.getTotalSkipped()
            );

            verify(parserService).parse(file);
            verify(validatorService).validate(parsedSpec);

            verify(mockSchemaService, times(2))
                    .createSchema(
                            eq(userId),
                            eq(projectId),
                            any(CreateMockSchemaRequest.class)
                    );
        }


        @Test
        @DisplayName("Should build correct schema creation request")
        void shouldBuildCorrectSchemaCreationRequest() {

            MultipartFile file = buildValidFile();
            ParsedOpenApiSpec parsedSpec = buildParsedSpec();

            when(projectRepository.findById(projectId))
                    .thenReturn(Optional.of(project));

            when(parserService.parse(file))
                    .thenReturn(parsedSpec);

            when(extractorService.extractSchema(anyString(), any()))
                    .thenReturn(
                            Map.of(
                                    "id",
                                    "uuid"
                            )
                    );

            when(mockSchemaService.createSchema(
                    any(),
                    any(),
                    any()
            )).thenReturn(
                    new MockSchemaResponse()
            );

            importService.importOpenApi(
                    userId,
                    projectId,
                    file
            );

            ArgumentCaptor<CreateMockSchemaRequest> captor =
                    ArgumentCaptor.forClass(
                            CreateMockSchemaRequest.class
                    );

            verify(mockSchemaService, atLeastOnce())
                    .createSchema(
                            eq(userId),
                            eq(projectId),
                            captor.capture()
                    );

            CreateMockSchemaRequest request =
                    captor.getValue();

            assertNotNull(request.getName());
            assertNotNull(request.getSchemaJson());
        }
    }


    @Nested
    @DisplayName("Partial Failure Tests")
    class PartialFailureTests {

        @Test
        @DisplayName("Should skip failed schemas and continue importing")
        void shouldSkipFailedSchemasAndContinue() {

            MultipartFile file = buildValidFile();
            ParsedOpenApiSpec parsedSpec = buildParsedSpec();

            when(projectRepository.findById(projectId))
                    .thenReturn(Optional.of(project));

            when(parserService.parse(file))
                    .thenReturn(parsedSpec);

            when(extractorService.extractSchema(
                    eq("User"),
                    any()
            )).thenThrow(
                    new BadRequestException("Invalid User schema")
            );

            when(extractorService.extractSchema(
                    eq("Product"),
                    any()
            )).thenReturn(
                    Map.of(
                            "name",
                            "string"
                    )
            );

            when(mockSchemaService.createSchema(
                    any(),
                    any(),
                    any()
            )).thenReturn(
                    new MockSchemaResponse()
            );

            OpenApiImportResponse response =
                    importService.importOpenApi(
                            userId,
                            projectId,
                            file
                    );

            assertEquals(
                    1,
                    response.getTotalImported()
            );

            assertEquals(
                    1,
                    response.getTotalSkipped()
            );

            assertEquals(
                    "User",
                    response.getSkipped()
                            .getFirst()
                            .component()
            );
        }
    }


    @Nested
    @DisplayName("Failure Tests")
    class FailureTests {

        @Test
        @DisplayName("Should reject missing project")
        void shouldRejectMissingProject() {

            MultipartFile file = buildValidFile();

            when(projectRepository.findById(projectId))
                    .thenReturn(Optional.empty());

            ResourceNotFoundException ex =
                    assertThrows(
                            ResourceNotFoundException.class,
                            () -> importService.importOpenApi(
                                    userId,
                                    projectId,
                                    file
                            )
                    );

            assertEquals(
                    "Project not found",
                    ex.getMessage()
            );
        }


        @Test
        @DisplayName("Should reject null file")
        void shouldRejectNullFile() {

            when(projectRepository.findById(projectId))
                    .thenReturn(Optional.of(project));

            BadRequestException ex =
                    assertThrows(
                            BadRequestException.class,
                            () -> importService.importOpenApi(
                                    userId,
                                    projectId,
                                    null
                            )
                    );

            assertEquals(
                    "OpenAPI file is required",
                    ex.getMessage()
            );
        }


        @Test
        @DisplayName("Should reject unsupported file type")
        void shouldRejectUnsupportedFileType() {

            MultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "invalid.txt",
                            "text/plain",
                            "bad".getBytes()
                    );

            when(projectRepository.findById(projectId))
                    .thenReturn(Optional.of(project));

            BadRequestException ex =
                    assertThrows(
                            BadRequestException.class,
                            () -> importService.importOpenApi(
                                    userId,
                                    projectId,
                                    file
                            )
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "Unsupported file type"
                    )
            );
        }


        @Test
        @DisplayName("Should reject oversized file")
        void shouldRejectOversizedFile() {

            byte[] oversized =
                    new byte[(2 * 1024 * 1024) + 1];

            MultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "large.yaml",
                            "application/x-yaml",
                            oversized
                    );

            when(projectRepository.findById(projectId))
                    .thenReturn(Optional.of(project));

            BadRequestException ex =
                    assertThrows(
                            BadRequestException.class,
                            () -> importService.importOpenApi(
                                    userId,
                                    projectId,
                                    file
                            )
                    );

            assertTrue(
                    ex.getMessage().contains(
                            "File size exceeds"
                    )
            );
        }


        @Test
        @DisplayName("Should propagate parser failure")
        void shouldPropagateParserFailure() {

            MultipartFile file = buildValidFile();

            when(projectRepository.findById(projectId))
                    .thenReturn(Optional.of(project));

            when(parserService.parse(file))
                    .thenThrow(
                            new InvalidOpenApiException(
                                    "Invalid spec"
                            )
                    );

            InvalidOpenApiException ex =
                    assertThrows(
                            InvalidOpenApiException.class,
                            () -> importService.importOpenApi(
                                    userId,
                                    projectId,
                                    file
                            )
                    );

            assertEquals(
                    "Invalid spec",
                    ex.getMessage()
            );
        }


        @Test
        @DisplayName("Should propagate validator failure")
        void shouldPropagateValidatorFailure() {

            MultipartFile file = buildValidFile();
            ParsedOpenApiSpec parsedSpec = buildParsedSpec();

            when(projectRepository.findById(projectId))
                    .thenReturn(Optional.of(project));

            when(parserService.parse(file))
                    .thenReturn(parsedSpec);

            doThrow(
                    new InvalidOpenApiException(
                            "Validation failed"
                    )
            ).when(validatorService)
                    .validate(parsedSpec);

            InvalidOpenApiException ex =
                    assertThrows(
                            InvalidOpenApiException.class,
                            () -> importService.importOpenApi(
                                    userId,
                                    projectId,
                                    file
                            )
                    );

            assertEquals(
                    "Validation failed",
                    ex.getMessage()
            );
        }
    }
}