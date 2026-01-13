package com.mockify.backend.dto.request.auth;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingRegistration {
    private String name;
    private String email;
    private String encodedPassword;
}
