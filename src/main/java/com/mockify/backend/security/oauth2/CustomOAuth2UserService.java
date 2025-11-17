package com.mockify.backend.security.oauth2;
import com.mockify.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /**
     - Loads user information from Google and maps it into Spring's OAuth2User.
     - No user creation happens here — handled later in success handler.
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = switch (provider.toLowerCase()) {
            case "google" -> new GoogleOAuth2UserInfo(oauth2User.getAttributes());
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };

        if (userInfo.getEmail() == null) {
            throw new IllegalArgumentException("Provider did not return an email");
        }

        // Just logs whether user is known or new
        userRepository.findByEmail(userInfo.getEmail())
                .ifPresentOrElse(
                        u -> log.info("Existing user via {}", provider),
                        () -> log.info("New OAuth user: {}", userInfo.getEmail())
                );

        // Return Spring-compatible OAuth2User
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                oauth2User.getAttributes(),
                userInfo.getId()
        );
    }
}
