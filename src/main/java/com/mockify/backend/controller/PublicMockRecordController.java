package com.mockify.backend.controller;

import com.mockify.backend.dto.response.record.MockRecordResponse;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.PublicMockRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/mock")
@RequiredArgsConstructor
@Slf4j
public class PublicMockRecordController {

    private final PublicMockRecordService publicMockRecordService;
    private final EndpointService endpointService;

    /**
     * Get a record by ID (Public/Free User)
     */
    @GetMapping("/{org}/{project}/{schema}/records/{recordId}")
    public ResponseEntity<MockRecordResponse> getRecord(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @PathVariable UUID recordId) {

        log.info("Public user fetching recordId={} for schemaId={}", recordId, schema);

        UUID schemaId = endpointService.resolveSchema(org, project, schema);
        MockRecordResponse record = publicMockRecordService.getRecordById(schemaId, recordId);
        return ResponseEntity.ok(record);
    }

    /**
     * Get all records under a schema (Public/Free User)
     */
    @GetMapping("/{org}/{project}/{schema}/records")
    public ResponseEntity<List<MockRecordResponse>> getRecords(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema) {

        log.info("Public user fetching all records for schemaId={}", schema);

        UUID schemaId = endpointService.resolveSchema(org, project, schema);
        List<MockRecordResponse> records = publicMockRecordService.getRecordsBySchemaId(schemaId);
        return ResponseEntity.ok(records);
    }
}
