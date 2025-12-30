package com.mockify.backend.service;

public interface MailService {

    // Send password reset link to register email
    void sendPasswordResetMail(String to, String resetLink);
}
