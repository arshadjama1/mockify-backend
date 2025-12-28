package com.mockify.backend.service.impl;

import com.mockify.backend.dto.response.AuthResult;
import com.mockify.backend.dto.response.TokenPair;
import com.mockify.backend.dto.request.auth.LoginRequest;
import com.mockify.backend.dto.request.auth.RegisterRequest;
import com.mockify.backend.dto.response.auth.AuthResponse;
import com.mockify.backend.dto.response.auth.UserResponse;
import com.mockify.backend.exception.DuplicateResourceException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.exception.UnauthorizedException;
import com.mockify.backend.mapper.UserMapper;
import com.mockify.backend.model.User;
import com.mockify.backend.repository.UserRepository;
import com.mockify.backend.security.CookieUtil;
import com.mockify.backend.security.JwtTokenProvider;
import com.mockify.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final CookieUtil cookieUtil;

    @Override
    @Transactional
    public AuthResult registerAndLogin(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("User registration failed email={} reason=already_exists", request.getEmail());
            throw new DuplicateResourceException("Email already registered");
        }

        // Create user
        log.info("User registration started email={}", request.getEmail());
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setProviderName("local");
        user.setUsername(request.getEmail().split("@")[0].toLowerCase());

        User savedUser = userRepository.save(user);
        log.info("User registered successfully userId={}", savedUser.getId());

        // Generate tokens
        TokenPair tokens = new TokenPair(
                jwtTokenProvider.generateAccessToken(savedUser.getId()),
                jwtTokenProvider.generateRefreshToken(savedUser.getId())
        );

        // Build cookie
        ResponseCookie cookie = cookieUtil.createRefreshToken(tokens.refreshToken());

        // Build response body
        AuthResponse response = AuthResponse.builder()
                .accessToken(tokens.accessToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .user(userMapper.toResponse(user))
                .build();


        return new AuthResult(response, cookie);
    }


    @Override
    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest request) {

        log.info("Login attempt for email: {}", request.getEmail());

        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Check if it's a local user (has password)
        if (user.getPassword() == null || user.getProviderName() == null || !"local".equals(user.getProviderName())) {
            throw new UnauthorizedException("This account uses OAuth login. Please login with " + user.getProviderName());
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        // Generate tokens
        TokenPair tokens = new TokenPair(
                jwtTokenProvider.generateAccessToken(user.getId()),
                jwtTokenProvider.generateRefreshToken(user.getId())
        );

        // Build cookie
        ResponseCookie cookie = cookieUtil.createRefreshToken(tokens.refreshToken());

        // Build response body
        AuthResponse response = AuthResponse.builder()
                .accessToken(tokens.accessToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .user(userMapper.toResponse(user))
                .build();

        log.info("Login successful userId={}", user.getId());

        return new AuthResult(response, cookie);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResult refresh(String refreshToken) {

        log.info("Token refresh requested");

        // Token validation
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token missing");
        }

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            log.warn("Token refresh failed reason=invalid_refresh_token");
            throw new UnauthorizedException("Invalid refresh token");
        }

        // User validation
        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Generate tokens
        TokenPair tokens = new TokenPair(
                jwtTokenProvider.generateAccessToken(userId),
                jwtTokenProvider.generateRefreshToken(userId)
        );

        // Build cookie
        ResponseCookie cookie = cookieUtil.createRefreshToken(tokens.refreshToken());

        // Build response body
        AuthResponse response = AuthResponse.builder()
                .accessToken(tokens.accessToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .build();

        log.info("Token refreshed userId={}", userId);

        return new AuthResult(response, cookie);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {

        log.debug("Fetching user profile userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return userMapper.toResponse(user);
    }

    public void logout() {

        log.info("Logout requested");

        // TODAY: no-op
        // FUTURE: redis token invalidation

        log.info("Logout completed");
    }
}
