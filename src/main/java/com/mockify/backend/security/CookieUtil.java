package com.mockify.backend.security;

import org.springframework.http.ResponseCookie;

public class CookieUtil {

    private static final String REFRESH_TOKEN = "refresh_token";

    public static ResponseCookie createRefreshToken(String token) {
        return ResponseCookie.from(REFRESH_TOKEN, token)
                .httpOnly(true)
                .secure(false)          // true in production
                .path("/api/v1/auth/refresh")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();
    }

    public static ResponseCookie clearRefreshToken() {
        return ResponseCookie.from(REFRESH_TOKEN, "")
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth/refresh")
                .maxAge(0)
                .build();
    }
}