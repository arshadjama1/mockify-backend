package com.mockify.backend.service;

import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;

public interface SchemaExtractorService {

    public Map<String, Object> extractSchema(String schemaName, Schema<?> schema);
}
