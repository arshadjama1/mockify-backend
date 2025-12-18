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
@RequestMapping("/api/v1/mock")
@RequiredArgsConstructor
@Slf4j
public class PublicMockRecordController {

    private final PublicMockRecordService publicMockRecordService;
    private final EndpointService endpointService;

    /**
     * Get a record by ID (Public/Free User)
     */
    @GetMapping("/schemas/{schemaId}/records/{recordId}")
    public ResponseEntity<MockRecordResponse> getRecordById(
            @PathVariable UUID schemaId,
            @PathVariable UUID recordId) {

        log.info("Public user fetching recordId={} for schemaId={}", recordId, schemaId);

        MockRecordResponse records = publicMockRecordService.getRecordById(schemaId, recordId);
        return ResponseEntity.ok(records);
    }

    /**
     * Get all records under a schema (Public/Free User)
     */
    @GetMapping("/schemas/{schemaId}/records")
    public ResponseEntity<List<MockRecordResponse>> getRecordsBySchema(
            @PathVariable UUID schemaId) {

        log.info("Public user fetching all records for schemaId={}", schemaId);

        List<MockRecordResponse> records = publicMockRecordService.getRecordsBySchemaId(schemaId);
        return ResponseEntity.ok(records);
    }

    // Slug-based public endpoint example (using direct slug lookups)
    @GetMapping("/{orgSlug}/{projectSlug}/{schemaSlug}/records")
    public ResponseEntity<List<MockRecordResponse>> getRecordsBySlug(
            @PathVariable String orgSlug,
            @PathVariable String projectSlug,
            @PathVariable String schemaSlug) {

        return ResponseEntity.ok(
                publicMockRecordService.getRecordsBySlug(orgSlug, projectSlug, schemaSlug)
        );
    }


    // SLUG-BASED ROUTES

    @GetMapping("/schemas/{schemaSlug}/records/{recordId}")
    public ResponseEntity getRecordById(
            @PathVariable String schemaSlug,
            @PathVariable UUID recordId) {
        UUID schemaId = endpointService.resolveSchemaId(schemaSlug);
        log.info("Public user fetching recordId={} for schemaId={}", recordId, schemaId);
        MockRecordResponse records = publicMockRecordService.getRecordById(schemaId, recordId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/schemas/{schemaSlug}/records")
    public ResponseEntity<List> getRecordsBySchema(
            @PathVariable String schemaSlug) {
        UUID schemaId = endpointService.resolveSchemaId(schemaSlug);
        log.info("Public user fetching all records for schemaId={}", schemaId);
        List records = publicMockRecordService.getRecordsBySchemaId(schemaId);
        return ResponseEntity.ok(records);
    }
}
