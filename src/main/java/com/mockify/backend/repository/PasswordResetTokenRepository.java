package com.mockify.backend.repository;

import com.mockify.backend.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, UUID> {

    List<PasswordResetToken> findByUsedFalseAndExpiresAtAfter(LocalDateTime now);
}
