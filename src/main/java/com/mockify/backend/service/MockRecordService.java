package com.mockify.backend.service;

import com.mockify.backend.dto.request.record.CreateMockRecordRequest;
import com.mockify.backend.dto.request.record.UpdateMockRecordRequest;
import com.mockify.backend.dto.response.record.MockRecordResponse;

import java.util.List;
import java.util.Optional;

public interface MockRecordService {

    MockRecordResponse createRecord(Long userId, CreateMockRecordRequest request);

    List<MockRecordResponse> createRecordsBulk(Long userId, List<CreateMockRecordRequest> requests);

    MockRecordResponse getRecordById(Long userId, Long recordId);

    List<MockRecordResponse> getRecordsBySchemaId(Long userId, Long schemaId);

    MockRecordResponse updateRecord(Long userId, Long recordId, UpdateMockRecordRequest request);

    void deleteRecord(Long userId, Long recordId);

    long countRecords();
}
