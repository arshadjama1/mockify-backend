package com.mockify.backend.service.impl;

import com.mockify.backend.dto.internal.ParsedOpenApiSpec;
import com.mockify.backend.dto.request.schema.CreateMockSchemaRequest;
import com.mockify.backend.dto.response.imports.OpenApiImportResponse;
import com.mockify.backend.dto.response.imports.SkippedSchema;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.model.Project;
import com.mockify.backend.repository.ProjectRepository;
import com.mockify.backend.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.models.media.Schema;

import java.util.*;

/**
 * OpenApiImportService
 *
 * Main orchestrator for OpenAPI import flow
 *
 * Full flow:
 * 1. Receive uploaded file
 * 2. Parse raw OpenAPI
 * 3. Validate specification
 * 4. Extract schemas
 * 5. Convert schemas into Mockify schemaJson
 * 6. Create actual Mock Schemas inside project
 * 7. Collect successes + failures
 * 8. Return final summary
 *
 * This service is responsible for business execution only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenApiImportServiceImpl implements OpenApiImportService {

    private final OpenApiParserService parserService;
    private final OpenApiValidatorService validatorService;
    private final SchemaExtractorService extractorService;

    private final MockSchemaService mockSchemaService;
    private final ProjectRepository projectRepository;

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2 MB

    /**
     * Import OpenAPI file and auto-generate mock schemas
     *
     * Security:
     * User must have schema write permission in project
     *
     * @param userId authenticated user
     * @param projectId target project
     * @param file uploaded swagger/openapi file
     * @return import summary
     */
    @Transactional
    @PreAuthorize("hasPermission(#projectId, 'PROJECT', 'SCHEMA:WRITE')")
    @Override
    public OpenApiImportResponse importOpenApi(
            UUID userId,
            UUID projectId,
            MultipartFile file
    ) {

        log.info(
                "User {} started OpenAPI import for project {}",
                userId,
                projectId
        );

        // Ensure project exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Project not found")
                );

        // validate the file
        validateFile(file);

        // Parse uploaded file
        ParsedOpenApiSpec parsedSpec = parserService.parse(file);

        // Validate parsed specification
        validatorService.validate(parsedSpec);

        // Store the results
        List<MockSchemaResponse> importedSchemas = new ArrayList<>();
        List<SkippedSchema> skippedSchemas = new ArrayList<>();

        // Iterate through all reusable OpenAPI schemas
        for (Map.Entry<String, Schema> entry :
                parsedSpec.getSchemas().entrySet()) {

            String componentName = entry.getKey();
            Schema<?> openApiSchema = entry.getValue();

            try {

                // Convert OpenAPI schema to Mockify schemaJson
                Map<String, Object> schemaJson =
                        extractorService.extractSchema(
                                componentName,
                                openApiSchema
                        );

                /*
                 * Build internal create request
                 *
                 * Original schema name becomes Mock Schema name
                 */
                CreateMockSchemaRequest request =
                        new CreateMockSchemaRequest();

                request.setName(componentName);
                request.setSchemaJson(schemaJson);

                /*
                 * Use existing schema creation flow
                 *
                 * This automatically:
                 * - validates schemaJson
                 * - creates slug
                 * - saves schema
                 * - creates endpoint
                 */
                MockSchemaResponse created =
                        mockSchemaService.createSchema(
                                userId,
                                project.getId(),
                                request
                        );

                importedSchemas.add(created);

                log.info(
                        "Imported OpenAPI schema '{}' into project {}",
                        componentName,
                        projectId
                );

            } catch (Exception ex) {

                /*
                 * Partial failure strategy:
                 * Skip invalid schema,
                 * continue importing others
                 */
                log.warn("Skipping OpenAPI schema '{}' due to error: {}", componentName, ex.getMessage());

                skippedSchemas.add(
                        new SkippedSchema(componentName, ex.getMessage())
                );
            }
        }

        // Final summary log
        log.info(
                "OpenAPI import completed for project {} | imported={} | skipped={}",
                projectId, importedSchemas.size(), skippedSchemas.size()
        );

        // Return summary response
        return OpenApiImportResponse.of(
                importedSchemas,
                skippedSchemas
        );
    }


    /**
     * Validates uploaded OpenAPI specification file before parsing.
     *
     * Validation Rules:
     * 1. File must be present and not empty
     * 2. File extension must be one of:
     *    - .yaml
     *    - .yml
     *    - .json
     * 3. File size must not exceed configured maximum limit
     *
     * @param file uploaded multipart OpenAPI specification file
     */
    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("OpenAPI file is required");
        }

        String filename = file.getOriginalFilename();

        // Convert the filename to lowercase before checking
        String normalizedFilename =
                filename == null ? null : filename.toLowerCase(Locale.ROOT);

        if (filename == null ||
                (!normalizedFilename.endsWith(".yaml") &&
                        !normalizedFilename.endsWith(".yml") &&
                        !normalizedFilename.endsWith(".json"))) {

            throw new BadRequestException(
                    "Unsupported file type. Please upload .yaml, .yml, or .json files only."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException(
                    "File size exceeds maximum allowed limit of 2 MB."
            );
        }
    }
}
