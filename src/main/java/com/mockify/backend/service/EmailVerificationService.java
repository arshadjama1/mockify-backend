package com.mockify.backend.service;

import com.mockify.backend.dto.request.auth.PendingRegistration;

public interface EmailVerificationService {

    String createVerification(
            String name,
            String email,
            String encodedPassword
    );

    PendingRegistration verifyAndConsume(String token);
}