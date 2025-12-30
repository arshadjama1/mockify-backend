package com.mockify.backend.service;

import com.mockify.backend.dto.response.AuthResult;
import com.mockify.backend.dto.request.auth.LoginRequest;
import com.mockify.backend.dto.request.auth.RegisterRequest;
import com.mockify.backend.dto.response.auth.UserResponse;

import java.util.UUID;

public interface AuthService {

    // Register a new user
    AuthResult registerAndLogin(RegisterRequest request);

    // Login with email & password
    AuthResult login(LoginRequest request);

    // Fetch details of currently authenticated user
    UserResponse getCurrentUser(UUID userId);

    // Logout user and invalidate tokens
    void logout();

    // Refresh access_token using refresh_token
    AuthResult refresh(String refreshToken);

    // Forget password request
    void forgotPassword(String email);

    // Reset password via mail
    void resetPassword(String token, String newPassword);

}
