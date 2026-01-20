package com.mockify.backend.dto.response.admin;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String name,
        String email,
        boolean emailVerified,
        String role,
        String providerName,
        String username,
        LocalDateTime createdAt
) {}

