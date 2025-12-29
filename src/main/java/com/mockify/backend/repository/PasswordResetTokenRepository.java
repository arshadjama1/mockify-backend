package com.mockify.backend.repository;

import com.mockify.backend.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(
            String tokenHash
    );

    List<PasswordResetToken> findAllByUserIdAndUsedFalse(UUID userId);
}