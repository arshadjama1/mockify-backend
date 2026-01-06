package com.mockify.backend.service;

import com.mockify.backend.repository.MockRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MockRecordCleanupService {

    private final MockRecordRepository mockRecordRepository;

    @Transactional
    public int cleanExpiredMockRecords() {
        return mockRecordRepository.deleteExpiredMockRecords(LocalDateTime.now());
    }
}