package com.mockify.backend.dto.response.auth;

import org.springframework.http.ResponseCookie;

public record AuthResult(
        AuthResponse response,
        ResponseCookie refreshCookie
) {}