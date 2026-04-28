package com.mockify.backend.service;

import com.mockify.backend.dto.response.record.MockRecordResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PublicMockRecordService {

    // Get record by ID (public user)
    MockRecordResponse getRecordById(UUID schemaId, UUID recordId);

    // Get all records under Aa schema (public user)
    Page<MockRecordResponse> getRecordsBySchemaId(UUID schemaId, Pageable pageable);

    Page<MockRecordResponse> getRecordsBySlug(
            String orgSlug,
            String projectSlug,
            String schemaSlug,
            Pageable pageable
    );
}
