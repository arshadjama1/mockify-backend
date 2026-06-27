package com.mockify.backend.dto.response.imports;

public record SkippedSchema(
        String component,
        String reason
) {}
