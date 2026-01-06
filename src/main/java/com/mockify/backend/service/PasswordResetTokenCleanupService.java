package com.mockify.backend.service;

import com.mockify.backend.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenCleanupService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Transactional
    public int cleanExpiredTokens(){

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusHours(24);

        return passwordResetTokenRepository.deleteExpiredTokens(now, cutoff);
    }
}
