package com.mockify.backend.dto.response.admin;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminMockSchemaResponse(
        UUID id,
        String name,
        String slug,
        UUID projectId,
        String projectName,
        int recordCount,
        LocalDateTime createdAt
) {}

