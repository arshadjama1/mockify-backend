// src/main/java/com/mockify/backend/scheduler/InvitationCleanupScheduler.java
package com.mockify.backend.scheduler;

import com.mockify.backend.repository.OrganizationInvitationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvitationCleanupScheduler {

    private final OrganizationInvitationRepository invitationRepo;

    @Value("${cleanup.invitations.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${cleanup.invitations.cron}")
    @Transactional
    public void cleanExpiredInvitations() {
        if (!enabled) return;
        try {
            int deleted = invitationRepo.deleteExpiredInvitations(LocalDateTime.now());
            if (deleted > 0) {
                log.info("[Cleanup] Deleted {} expired invitations", deleted);
            }
        } catch (Exception ex) {
            log.error("[Cleanup] Invitation cleanup failed", ex);
        }
    }

    @PostConstruct
    public void init() {
        log.info("InvitationCleanupScheduler initialized");
    }
}