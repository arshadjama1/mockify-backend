package com.mockify.backend.service.impl;

import com.mockify.backend.common.validation.PageableValidator;
import com.mockify.backend.dto.request.record.CreateMockRecordRequest;
import com.mockify.backend.dto.request.record.UpdateMockRecordRequest;
import com.mockify.backend.dto.response.record.MockRecordResponse;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.mapper.MockRecordMapper;
import com.mockify.backend.model.MockRecord;
import com.mockify.backend.model.MockSchema;
import com.mockify.backend.repository.MockRecordRepository;
import com.mockify.backend.repository.MockSchemaRepository;
import com.mockify.backend.service.MockAutoGenerateService;
import com.mockify.backend.service.MockRecordService;
import com.mockify.backend.service.MockValidatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockRecordServiceImpl implements MockRecordService {

    private final MockRecordRepository mockRecordRepository;
    private final MockSchemaRepository mockSchemaRepository;
    private final MockRecordMapper mockRecordMapper;
    private final MockValidatorService mockValidatorService;
    private final MockAutoGenerateService autoGenerateService;

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#schemaId, 'SCHEMA', 'RECORD:WRITE')")
    public MockRecordResponse createRecord(UUID userId, UUID schemaId, CreateMockRecordRequest request) {

        if (request == null) throw new BadRequestException("Request cannot be null");
        if (schemaId == null) throw new BadRequestException("Schema ID is required");
        if (request.getData() == null) throw new BadRequestException("Record data cannot be null");

        MockSchema schema = getSchemaOrThrow(schemaId);

        mockValidatorService.validateRecordAgainstSchema(schema.getSchemaJson(), request.getData());

        MockRecordResponse response = persistRecord(schema, request);

        log.info("Record created in schema {} by user {}", schemaId, userId);
        return response;
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#schemaId, 'SCHEMA', 'RECORD:WRITE')")
    public List<MockRecordResponse> createRecordsBulk(UUID userId, UUID schemaId, List<CreateMockRecordRequest> requests) {

        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("Records list cannot be null or empty");
        }

        MockSchema schema = getSchemaOrThrow(schemaId);

        log.info("Bulk creating {} records in schema {} by user {}", requests.size(), schemaId, userId);

        return requests.stream()
                .map(req -> {
                    if (req == null || req.getData() == null) {
                        throw new BadRequestException("Record data cannot be null");
                    }

                    mockValidatorService.validateRecordAgainstSchema(
                            schema.getSchemaJson(), req.getData());

                    return persistRecord(schema, req);
                })
                .toList();
    }

    // -------------------------------------------------------------------------
    // AUTO GENERATE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#schemaId, 'SCHEMA', 'RECORD:WRITE')")
    public List<MockRecordResponse> autoGenerateRecordsBulk(UUID userId, UUID schemaId, int count) {

        if (count <= 0) {
            throw new BadRequestException("Count must be greater than 0");
        }

        MockSchema schema = getSchemaOrThrow(schemaId);
        Map<String, Object> schemaJson = schema.getSchemaJson();

        // Validate schema once
        mockValidatorService.validateSchemaDefinition(schemaJson);

        log.info("Auto-generating {} records for schema {} by user {}", count, schemaId, userId);

        List<CreateMockRecordRequest> requests = new ArrayList<>();

        for (int i = 0; i < count; i++) {

            Map<String, Object> record =
                    autoGenerateService.generateRecord(schemaJson);

            // Validate generated record
            mockValidatorService.validateRecordAgainstSchema(schemaJson, record);

            CreateMockRecordRequest req = new CreateMockRecordRequest();
            req.setData(record);

            requests.add(req);
        }

        return createRecordsBulk(userId, schemaId, requests);
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#recordId, 'RECORD', 'READ')")
    public MockRecordResponse getRecordById(UUID userId, UUID recordId) {

        MockRecord record = getRecordOrThrow(recordId);
        return mockRecordMapper.toResponse(record);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#schemaId, 'SCHEMA', 'RECORD:READ')")
    public Page<MockRecordResponse> getRecordsBySchemaId(UUID userId, UUID schemaId, Pageable pageable) {

        PageableValidator.validate(pageable, 50);

        Page<MockRecord> page =
                mockRecordRepository.findByMockSchema_Id(schemaId, pageable);

        return page.map(mockRecordMapper::toResponse);
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#recordId, 'RECORD', 'WRITE')")
    public MockRecordResponse updateRecord(UUID userId, UUID recordId, UpdateMockRecordRequest request) {

        if (request == null) {
            throw new BadRequestException("Request cannot be null");
        }

        MockRecord record = getRecordOrThrow(recordId);

        if (request.getData() != null) {
            mockValidatorService.validateRecordAgainstSchema(
                    record.getMockSchema().getSchemaJson(),
                    request.getData()
            );
        }

        mockRecordMapper.updateEntityFromRequest(request, record);
        mockRecordRepository.save(record);

        log.info("Record {} updated by user {}", recordId, userId);
        return mockRecordMapper.toResponse(record);
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#recordId, 'RECORD', 'DELETE')")
    public void deleteRecord(UUID userId, UUID recordId) {

        MockRecord record = getRecordOrThrow(recordId);
        mockRecordRepository.delete(record);

        log.warn("Record {} deleted by user {}", recordId, userId);
    }

    // -------------------------------------------------------------------------
    // INTERNAL HELPERS
    // -------------------------------------------------------------------------

    private MockSchema getSchemaOrThrow(UUID schemaId) {
        return mockSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));
    }

    private MockRecord getRecordOrThrow(UUID recordId) {
        return mockRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));
    }

    private MockRecordResponse persistRecord(MockSchema schema, CreateMockRecordRequest request) {

        MockRecord record = mockRecordMapper.toEntity(request);
        record.setMockSchema(schema);
        record.setCreatedAt(LocalDateTime.now());
        record.setExpiresAt(LocalDateTime.now().plusDays(7));

        mockRecordRepository.save(record);

        return mockRecordMapper.toResponse(record);
    }

    // -------------------------------------------------------------------------
    // OPTIONAL: CLEANUP JOB
    // -------------------------------------------------------------------------

    @Transactional
    void deleteExpiredRecords() {
        List<MockRecord> expired =
                mockRecordRepository.findByExpiresAtBefore(LocalDateTime.now());

        mockRecordRepository.deleteAll(expired);

        log.info("Expired records deleted count={}", expired.size());
    }

    @Override
    @Transactional(readOnly = true)
    public long countRecords() {
        return mockRecordRepository.count();
    }
}