package com.mockify.backend.service;

import com.mockify.backend.dto.response.record.MockRecordResponse;
import java.util.List;
import java.util.UUID;

public interface PublicMockRecordService {

    // Get record by ID (public user)
    MockRecordResponse getRecordById(UUID schemaId, UUID recordId);

    // Get all records under Aa schema (public user)
    List<MockRecordResponse> getRecordsBySchemaId(UUID schemaId);

    List<MockRecordResponse> getRecordsBySlug(
            String orgSlug,
            String projectSlug,
            String schemaSlug
    );
}
