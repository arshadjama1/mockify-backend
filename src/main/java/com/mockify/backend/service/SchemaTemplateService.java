package com.mockify.backend.service;

import com.mockify.backend.dto.request.schema.CreateSchemaTemplateRequest;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import com.mockify.backend.dto.response.schema.SchemaTemplateResponse;

import java.util.List;
import java.util.UUID;

public interface SchemaTemplateService {


    List<SchemaTemplateResponse> getSystemTemplates();


    MockSchemaResponse applyTemplateToProject(UUID userId, UUID projectId, String templateSlug);
}
