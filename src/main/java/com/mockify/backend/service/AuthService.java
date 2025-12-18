package com.mockify.backend.service;

import com.mockify.backend.dto.request.auth.LoginRequest;
import com.mockify.backend.dto.request.auth.RegisterRequest;
import com.mockify.backend.dto.response.auth.AuthResponse;
import com.mockify.backend.dto.response.auth.UserResponse;
import com.mockify.backend.service.impl.AuthServiceImpl;
import org.springframework.http.ResponseCookie;

import java.util.UUID;

public interface AuthService {

    // Register a new user
    public AuthServiceImpl.TokenPair registerAndLogin(RegisterRequest request);

    // Login with email & password
    AuthServiceImpl.TokenPair login(LoginRequest request);

    // Fetch details of currently authenticated user
    UserResponse getCurrentUser(UUID userId);

    // Logout user and invalidate tokens
    void logout();

    // Refresh access_token using refresh_token
    public String refreshAccessToken(String refreshToken);

//     Change current user's password
//  void changePassword(String oldPassword, String newPassword);

}
