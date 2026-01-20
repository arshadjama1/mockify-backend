package com.mockify.backend.dto.response.admin;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminOrganizationResponse(
        UUID id,
        String name,
        String slug,
        UUID ownerId,
        String ownerName,
        int projectCount,
        LocalDateTime createdAt
) {}

