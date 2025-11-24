package com.mockify.backend.controller;

import com.mockify.backend.dto.response.record.MockRecordResponse;
import com.mockify.backend.service.PublicMockRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Slf4j
public class PublicMockRecordController {

    private final PublicMockRecordService publicMockRecordService;

    /**
     * Get a record by ID (Public/Free User)
     */
    @GetMapping("/schemas/{schemaId}/records/{recordId}")
    public ResponseEntity<MockRecordResponse> getRecordById(
            @PathVariable Long schemaId,
            @PathVariable Long recordId) {

        log.info("Public user fetching recordId={} for schemaId={}", recordId, schemaId);

        MockRecordResponse record = publicMockRecordService.getRecordById(schemaId, recordId);
        return ResponseEntity.ok(record);
    }

    /**
     * Get all records under a schema (Public/Free User)
     */
    @GetMapping("/schemas/{schemaId}/records")
    public ResponseEntity<List<MockRecordResponse>> getRecordsBySchema(
            @PathVariable Long schemaId) {

        log.info("Public user fetching all records for schemaId={}", schemaId);

        List<MockRecordResponse> records = publicMockRecordService.getRecordsBySchemaId(schemaId);
        return ResponseEntity.ok(records);
    }
}
