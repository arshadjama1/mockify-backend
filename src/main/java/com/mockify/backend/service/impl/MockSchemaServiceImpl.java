package com.mockify.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockify.backend.dto.request.schema.CreateMockSchemaRequest;
import com.mockify.backend.dto.request.schema.UpdateMockSchemaRequest;
import com.mockify.backend.dto.response.schema.MockSchemaDetailResponse;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.exception.DuplicateResourceException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.mapper.MockSchemaMapper;
import com.mockify.backend.model.MockSchema;
import com.mockify.backend.model.Project;
import com.mockify.backend.repository.MockSchemaRepository;
import com.mockify.backend.repository.OrganizationRepository;
import com.mockify.backend.repository.ProjectRepository;
import com.mockify.backend.service.AccessControlService;
import com.mockify.backend.service.MockSchemaService;
import com.mockify.backend.service.MockValidatorService;
import com.mockify.backend.service.SlugService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockSchemaServiceImpl implements MockSchemaService {

    private final MockSchemaRepository mockSchemaRepository;
    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final MockSchemaMapper mockSchemaMapper;
    private final ObjectMapper objectMapper;
    private final MockValidatorService mockValidatorService;
    private final AccessControlService accessControlService;
    private final SlugService slugService;

    // Utility method to fetch project with ownership validation
    private Project getProjectWithAccessCheck(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        accessControlService.checkOrganizationAccess(userId, project.getOrganization(), "Project");
        return project;
    }

    // Utility method to fetch schema with ownership validation
    private MockSchema getSchemaWithAccessCheck(UUID schemaId, UUID userId) {
        MockSchema schema = mockSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));

        accessControlService.checkOrganizationAccess(userId, schema.getProject().getOrganization(), "Schema");
        return schema;
    }

    /*
      Create a new mock schema under a specific project
      Only the organization owner can create schemas
     */
    @Override
    @Transactional
    public MockSchemaResponse createSchema(UUID userId, CreateMockSchemaRequest request) {
        Project project = getProjectWithAccessCheck(request.getProjectId(), userId);

        // Prevent duplicate schema name in the same project
        boolean exists = mockSchemaRepository.findByNameAndProjectId(request.getName(), project.getId()) != null;
        if (exists) {
            throw new DuplicateResourceException("Schema with the same name already exists in this project");
        }

        // Generate slug from name
        String slug = slugService.generateSlug(request.getName());

        // Check uniqueness within project
        if (mockSchemaRepository.existsBySlugAndProjectId(slug, request.getProjectId())) {
           slugService.generateUniqueSlug(slug);
        }

        // Validate Mock Schema
        mockValidatorService.validateSchemaDefinition(request.getSchemaJson());

        MockSchema schema = mockSchemaMapper.toEntity(request);
        schema.setProject(project);
        schema.setSlug(slug);

        MockSchema saved = mockSchemaRepository.save(schema);
        return mockSchemaMapper.toResponse(saved);
    }

    /*
      Fetch all schemas under a project
      Only the org owner can view them
     */
    @Override
    @Transactional(readOnly = true)
    public List<MockSchemaResponse> getSchemasByProjectId(UUID userId, UUID projectId) {
        getProjectWithAccessCheck(projectId, userId);
        List<MockSchema> schemas = mockSchemaRepository.findByProjectId(projectId);
        return mockSchemaMapper.toResponseList(schemas);
    }

    /*
      Fetch a specific schema
     */
    @Override
    @Transactional(readOnly = true)
    public MockSchemaDetailResponse getSchemaById(UUID userId, UUID schemaId) {
        MockSchema schema = getSchemaWithAccessCheck(schemaId, userId);
        return mockSchemaMapper.toDetailResponse(schema);
    }

    /*
      Update schema (name or schema JSON)
      Ensures unique name and valid ownership
     */
    @Override
    @Transactional
    public MockSchemaResponse updateSchema(UUID userId, UUID schemaId, UpdateMockSchemaRequest request) {
        MockSchema schema = getSchemaWithAccessCheck(schemaId, userId);

        // Check if new name conflicts with another schema in same project
        if (request.getName() != null && !request.getName().equals(schema.getName())) {

            MockSchema existing = mockSchemaRepository.findByNameAndProjectId(
                    request.getName(),
                    schema.getProject().getId()
            );

            if (existing != null && !existing.getId().equals(schema.getId())) {
                throw new DuplicateResourceException("Schema with this name already exists");
            }
        }

        // Validate Mock Schema
        mockValidatorService.validateSchemaDefinition(request.getSchemaJson());

        mockSchemaMapper.updateEntityFromRequest(request, schema);

        // If name changed, update slug
        if (request.getName() != null && !request.getName().equals(schema.getName())) {
            String newSlug = slugService.generateSlug(request.getName());
            if (mockSchemaRepository.existsBySlugAndProjectId(newSlug, schema.getProject().getId())) {
                throw new DuplicateResourceException("Schema slug already exists in this project");
            }
            schema.setSlug(newSlug);
        }

        mockSchemaRepository.save(schema);
        return mockSchemaMapper.toResponse(schema);
    }

    /*
      Delete schema permanently
     */
    @Override
    @Transactional
    public void deleteSchema(UUID userId, UUID schemaId) {
        MockSchema schema = getSchemaWithAccessCheck(schemaId, userId);
        mockSchemaRepository.delete(schema);
    }

    /*
      Return total schema count (for admin/stats)
     */
    @Override
    public long countSchemas() {
        return mockSchemaRepository.count();
    }
}
