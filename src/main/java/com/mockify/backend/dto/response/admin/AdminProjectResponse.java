package com.mockify.backend.dto.response.admin;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminProjectResponse(
        UUID id,
        String name,
        String slug,
        UUID organizationId,
        String organizationName,
        int schemaCount,
        LocalDateTime createdAt
) {}

