package com.mockify.backend.service;

import com.mockify.backend.dto.request.schema.CreateMockSchemaRequest;
import com.mockify.backend.dto.request.schema.UpdateMockSchemaRequest;
import com.mockify.backend.dto.response.schema.MockSchemaDetailResponse;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MockSchemaService {

    //Create new schema under a project
    MockSchemaResponse createSchema(UUID userId, UUID projectId, CreateMockSchemaRequest request);

    // Get all schemas for a project
    List<MockSchemaResponse> getSchemasByProjectId(UUID userId, UUID projectId);

    // Get a schema by its ID
    MockSchemaDetailResponse getSchemaById(UUID userId, UUID schemaId);

    // Update schema definition or name
    MockSchemaResponse updateSchema(UUID userId, UUID schemaId, UpdateMockSchemaRequest request);

    // Delete schema and its records
    void deleteSchema(UUID userId, UUID schemaId);

    // Count all schemas
    long countSchemas();
}
