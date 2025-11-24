package com.mockify.backend.service;

import com.mockify.backend.dto.response.record.MockRecordResponse;
import java.util.List;

public interface PublicMockRecordService {

    // Get record by ID (public user)
    MockRecordResponse getRecordById(Long schemaId, Long recordId);

    // Get all records under Aa schema (public user)
    List<MockRecordResponse> getRecordsBySchemaId(Long schemaId);
}
