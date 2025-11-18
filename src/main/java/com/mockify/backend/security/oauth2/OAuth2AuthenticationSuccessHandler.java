package com.mockify.backend.security.oauth2;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockify.backend.mapper.UserMapper;
import com.mockify.backend.model.User;
import com.mockify.backend.repository.UserRepository;
import com.mockify.backend.dto.response.auth.AuthResponse;
import com.mockify.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    /**
     Called when Google login is successful.
     * Responsible for:
      - creating user (if first time)
      - generating JWT access + refresh tokens
     - returning JSON response to frontend
     */

    @Value("${app.frontend.url}")
    private String FRONTEND_URL;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        if (email == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided by provider");
            return;
        }

        // Create or update user record
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name != null ? name : email);
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // not used for OAuth
            return userRepository.save(newUser);
        });

        // Issue JWTs
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());

        // TODO: SECURITY - Set token in HttpOnly cookie instead of URL params
        String encodedAccessToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        String redirectUrl = String.format(
                "%s/oauth2/redirect?access_token=%s&expires_in=%d",
                FRONTEND_URL,
                encodedAccessToken,
                jwtTokenProvider.getAccessTokenExpiration()
        );
        response.sendRedirect(redirectUrl);
    }
}