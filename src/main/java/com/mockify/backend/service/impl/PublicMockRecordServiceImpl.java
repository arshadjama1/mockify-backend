package com.mockify.backend.service.impl;

import com.mockify.backend.dto.response.record.MockRecordResponse;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.mapper.MockRecordMapper;
import com.mockify.backend.model.MockRecord;
import com.mockify.backend.repository.MockRecordRepository;
import com.mockify.backend.service.PublicMockRecordService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicMockRecordServiceImpl implements PublicMockRecordService {

    private final MockRecordRepository mockRecordRepository;
    private final MockRecordMapper mockRecordMapper;

    @Override
    @Transactional(readOnly = true)
    public MockRecordResponse getRecordById(UUID schemaId, UUID recordId) {
        log.info("Public user requesting recordId={} for schemaId={}", recordId, schemaId);

        MockRecord record = mockRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));

        // Ensure schemaId matches
        if (!record.getMockSchema().getId().equals(schemaId)) {
            throw new ResourceNotFoundException("Record does not belong to the given schema");
        }

        return mockRecordMapper.toResponse(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MockRecordResponse> getRecordsBySchemaId(UUID schemaId) {
        log.info("Public user requesting all records for schemaId={}", schemaId);

        List<MockRecord> records = mockRecordRepository.findByMockSchema_Id(schemaId);

        return mockRecordMapper.toResponseList(records);
    }
}
