package com.mockify.backend.service.impl;

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
    public record TokenPair(String accessToken, String refreshToken) {}

    @Override
    @Transactional
    public TokenPair registerAndLogin(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setProviderName("local");
        user.setUsername(request.getEmail().split("@")[0].toLowerCase());

        User savedUser = userRepository.save(user);

        return new TokenPair(
                jwtTokenProvider.generateAccessToken(savedUser.getId()),
                jwtTokenProvider.generateRefreshToken(savedUser.getId())
        );
    }


    @Override
    @Transactional(readOnly = true)
    public TokenPair login(LoginRequest request) {
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
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        log.info("User logged in successfully: {}", user.getId());
        return new TokenPair(accessToken, refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public String refreshAccessToken(String refreshToken) {

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        return jwtTokenProvider.generateAccessToken(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toResponse(user);
    }

    @Override
    public void logout() {
        // Clear the Cookie
    }
}
