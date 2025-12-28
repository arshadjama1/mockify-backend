package com.mockify.backend.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.cookie.refresh-token")
@Component
@Getter
@Setter
public class CookieUtil {

    @NotBlank
    private String name;
    @NotBlank
    private String path;
    @Min(1)
    private long maxAge;
    private boolean secure;
    @NotBlank
    private String sameSite;

    public ResponseCookie createRefreshToken(String token) {
        return ResponseCookie.from(name, token)
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .maxAge(maxAge)
                .sameSite(sameSite)
                .build();
    }

    public ResponseCookie clearRefreshToken() {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .maxAge(0) // To delete a cookie, the browser requires 0
                .sameSite(sameSite)
                .build();
    }
}