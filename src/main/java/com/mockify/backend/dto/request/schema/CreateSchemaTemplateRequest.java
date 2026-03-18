package com.mockify.backend.dto.request.schema;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class CreateSchemaTemplateRequest {

    private String name;
    private String description;
    private Map<String, Object> schemaJson;
    private UUID projectId;
    private String templateSlug;
    private String projectSlug;
}
