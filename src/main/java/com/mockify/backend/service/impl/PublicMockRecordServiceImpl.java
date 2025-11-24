package com.mockify.backend.service.impl;

import com.mockify.backend.dto.response.record.MockRecordResponse;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.mapper.MockRecordMapper;
import com.mockify.backend.model.MockRecord;
import com.mockify.backend.model.MockSchema;
import com.mockify.backend.repository.MockRecordRepository;
import com.mockify.backend.repository.MockSchemaRepository;
import com.mockify.backend.service.PublicMockRecordService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicMockRecordServiceImpl implements PublicMockRecordService {

    private final MockRecordRepository mockRecordRepository;
    private final MockSchemaRepository mockSchemaRepository;
    private final MockRecordMapper mockRecordMapper;

    // Hardcoded public schema IDs
    private static final Set<Long> PUBLIC_SCHEMA_IDS = Set.of(1L, 2L, 5L);

    @Override
    @Transactional(readOnly = true)
    public MockRecordResponse getRecordById(Long schemaId, Long recordId) {
        log.info("Public user requesting recordId={} for schemaId={}", recordId, schemaId);

        if (!PUBLIC_SCHEMA_IDS.contains(schemaId)) {
            throw new ResourceNotFoundException("This schema is private");
        }

        MockSchema schema = mockSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));

        MockRecord record = mockRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));

        if (!record.getMockSchema().getId().equals(schemaId)) {
            throw new ResourceNotFoundException("Record does not belong to this schema");
        }

        return mockRecordMapper.toResponse(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MockRecordResponse> getRecordsBySchemaId(Long schemaId) {
        log.info("Public user requesting all records for schemaId={}", schemaId);

        if (!PUBLIC_SCHEMA_IDS.contains(schemaId)) {
            throw new ResourceNotFoundException("This schema is private");
        }

        MockSchema schema = mockSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));

        List<MockRecord> records = mockRecordRepository.findByMockSchema_Id(schemaId);

        return mockRecordMapper.toResponseList(records);
    }
}
