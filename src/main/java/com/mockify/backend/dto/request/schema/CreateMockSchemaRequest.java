package com.mockify.backend.dto.request.schema;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class CreateMockSchemaRequest {
    private String name;
    private Map<String, Object> schemaJson;
}
