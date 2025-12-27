package com.mockify.backend.dto.response;

import com.mockify.backend.dto.response.auth.AuthResponse;
import org.springframework.http.ResponseCookie;

public record AuthResult(
        AuthResponse response,
        ResponseCookie refreshCookie
) {}