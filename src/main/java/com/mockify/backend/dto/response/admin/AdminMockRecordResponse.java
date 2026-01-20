package com.mockify.backend.dto.response.admin;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminMockRecordResponse(
        UUID id,
        UUID schemaId,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {}
