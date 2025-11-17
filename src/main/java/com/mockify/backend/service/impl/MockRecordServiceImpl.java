package com.mockify.backend.service.impl;

import com.mockify.backend.dto.request.record.CreateMockRecordRequest;
import com.mockify.backend.dto.request.record.UpdateMockRecordRequest;
import com.mockify.backend.dto.response.record.MockRecordResponse;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.exception.ForbiddenException;
import com.mockify.backend.mapper.MockRecordMapper;
import com.mockify.backend.model.MockRecord;
import com.mockify.backend.model.MockSchema;
import com.mockify.backend.repository.MockRecordRepository;
import com.mockify.backend.repository.MockSchemaRepository;
import com.mockify.backend.service.MockRecordService;
import com.mockify.backend.service.MockValidatorService;
import com.mockify.backend.service.AccessControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockRecordServiceImpl implements MockRecordService {

    private final MockRecordRepository mockRecordRepository;
    private final MockSchemaRepository mockSchemaRepository;
    private final MockRecordMapper mockRecordMapper;
    private final MockValidatorService mockValidatorService;
    private final AccessControlService accessControlService;

    @Override
    @Transactional
    public MockRecordResponse createRecord(Long userId, CreateMockRecordRequest request) {
        log.info("User {} creating new mock record for schemaId={}", userId,
                request != null ? request.getSchemaId() : null);

        
        if (request == null) {
            throw new BadRequestException("Request cannot be null");
        }
        if (request.getSchemaId() == null) {
            throw new BadRequestException("Schema ID is required");
        }
        if (request.getData() == null) {
            throw new BadRequestException("Record data cannot be null");
        }

        MockSchema schema = mockSchemaRepository.findById(request.getSchemaId())
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));

        // check organization ownership via schema -> organization
        accessControlService.checkOrganizationAccess(userId, schema.getProject().getOrganization(), "Record");

        // VALIDATE DATA
        Map<String, Object> schemaJson = schema.getSchemaJson();
        mockValidatorService.validateRecordAgainstSchema(schemaJson, request.getData());

        MockRecord record = mockRecordMapper.toEntity(request);
        record.setMockSchema(schema);
        record.setCreatedAt(LocalDateTime.now());
        record.setExpiresAt(LocalDateTime.now().plusDays(7));

        mockRecordRepository.save(record);
        return mockRecordMapper.toResponse(record);
    }

    @Override
    @Transactional
    public List<MockRecordResponse> createRecordsBulk(Long userId, List<CreateMockRecordRequest> requests) {
        log.info("Bulk create requested by userId={} count={}", userId, requests == null ? 0 : requests.size());

        if (requests == null) {
            throw new BadRequestException("Records list cannot be null");
        }
        if (requests.isEmpty()) {
            throw new BadRequestException("Records list cannot be empty");
        }
        
        return requests.stream()
                .map(req -> createRecord(userId, req))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MockRecordResponse getRecordById(Long userId, Long recordId) {
        log.debug("Fetching record for userId={}, recordId={}", userId, recordId);

        MockRecord record = mockRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));

        accessControlService.checkOrganizationAccess(userId, record.getMockSchema().getProject().getOrganization(), "Record");

        return mockRecordMapper.toResponse(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MockRecordResponse> getRecordsBySchemaId(Long userId, Long schemaId) {
        log.debug("Fetching records for userId={}, schemaId={}", userId, schemaId);

        MockSchema schema = mockSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));

        accessControlService.checkOrganizationAccess(userId, schema.getProject().getOrganization(), "Record");

        List<MockRecord> records = mockRecordRepository.findByMockSchema_Id(schemaId);
        return mockRecordMapper.toResponseList(records);
    }

    @Override
    @Transactional
    public MockRecordResponse updateRecord(Long userId, Long recordId, UpdateMockRecordRequest request) {
        log.info("Updating record userId={}, recordId={}", userId, recordId);

        if (request == null) {
            throw new BadRequestException("Request cannot be null");
        }

        MockRecord record = mockRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));

        accessControlService.checkOrganizationAccess(userId, record.getMockSchema().getProject().getOrganization(), "Record");

        if (request.getData() != null) {
            mockValidatorService.validateRecordAgainstSchema(record.getMockSchema().getSchemaJson(), request.getData());
        }

        mockRecordMapper.updateEntityFromRequest(request, record);
        mockRecordRepository.save(record);

        return mockRecordMapper.toResponse(record);
    }

    @Override
    @Transactional
    public void deleteRecord(Long userId, Long recordId) {
        log.warn("Deleting record userId={}, recordId={}", userId, recordId);

        MockRecord record = mockRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));

        accessControlService.checkOrganizationAccess(userId, record.getMockSchema().getProject().getOrganization(), "Record");

        mockRecordRepository.delete(record);
    }

    @Transactional
    void deleteExpiredRecords() {
        log.info("Deleting expired records...");
        List<MockRecord> expired = mockRecordRepository.findByExpiresAtBefore(LocalDateTime.now());
        mockRecordRepository.deleteAll(expired);
        log.info("Expired records deleted count={}", expired.size());
    }

    @Override
    @Transactional(readOnly = true)
    public long countRecords() {
        long count = mockRecordRepository.count();
        log.debug("Total mock records count={}", count);
        return count;
    }
}
