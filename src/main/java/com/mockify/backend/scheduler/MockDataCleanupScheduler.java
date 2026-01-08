package com.mockify.backend.scheduler;

import com.mockify.backend.service.MockRecordCleanupService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockDataCleanupScheduler {

    private final MockRecordCleanupService cleanupService;

    @Value("${cleanup.mock-data.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${cleanup.mock-data.cron}")
    public void cleanExpiredMockData() {
        if (!enabled) {
            return;
        }

        try {
            long start = System.currentTimeMillis();
            int deleted = cleanupService.cleanExpiredMockRecords();
            long duration = System.currentTimeMillis() - start;

            log.info("[Cleanup] Deleted {} expired mock records in {} ms", deleted, duration);
        } catch (Exception ex) {
            log.error("[Cleanup] Mock data cleanup failed", ex);
        }
    }

    // For Debugging
    @PostConstruct
    public void init() {
        log.info("MockDataCleanupScheduler initialized");
    }
}