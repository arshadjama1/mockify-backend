package com.mockify.backend.controller;

import com.mockify.backend.dto.request.record.AutoGenerateRequest;
import com.mockify.backend.dto.request.record.CreateMockRecordRequest;
import com.mockify.backend.dto.request.record.UpdateMockRecordRequest;
import com.mockify.backend.dto.response.record.MockRecordResponse;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.MockRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class MockRecordController {

    private final MockRecordService mockRecordService;
    private final EndpointService endpointService;

    private UUID extractUserId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }

    private UUID resolveSchemaId(String org, String project, String schema) {
        return endpointService.resolveSchema(org, project, schema);
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @PostMapping("/{org}/{project}/{schema}/records")
    public ResponseEntity<MockRecordResponse> createRecord(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @Valid @RequestBody CreateMockRecordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);
        UUID schemaId = resolveSchemaId(org, project, schema);

        MockRecordResponse created =
                mockRecordService.createRecord(userId, schemaId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -------------------------------------------------------------------------
    // BULK CREATE
    // -------------------------------------------------------------------------

    @PostMapping("/{org}/{project}/{schema}/records/bulk")
    public ResponseEntity<List<MockRecordResponse>> createRecordsBulk(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @Valid @RequestBody List<CreateMockRecordRequest> requests,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);
        UUID schemaId = resolveSchemaId(org, project, schema);

        List<MockRecordResponse> created =
                mockRecordService.createRecordsBulk(userId, schemaId, requests);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -------------------------------------------------------------------------
    // AUTO GENERATE
    // -------------------------------------------------------------------------

    @PostMapping("/{org}/{project}/{schema}/records/auto-bulk")
    public ResponseEntity<List<MockRecordResponse>> autoGenerateRecordsBulk(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @Valid @RequestBody AutoGenerateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);
        UUID schemaId = resolveSchemaId(org, project, schema);

        List<MockRecordResponse> records =
                mockRecordService.autoGenerateRecordsBulk(
                        userId,
                        schemaId,
                        request.getCount()
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(records);
    }

    // -------------------------------------------------------------------------
    // READ (PAGINATED)
    // -------------------------------------------------------------------------

    @GetMapping("/{org}/{project}/{schema}/records")
    public ResponseEntity<Page<MockRecordResponse>> getRecords(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);
        UUID schemaId = resolveSchemaId(org, project, schema);

        Page<MockRecordResponse> page =
                mockRecordService.getRecordsBySchemaId(userId, schemaId, pageable);

        return ResponseEntity.ok(page);
    }

    // -------------------------------------------------------------------------
    // GET BY ID
    // -------------------------------------------------------------------------

    @GetMapping("/{org}/{project}/{schema}/records/{recordId}")
    public ResponseEntity<MockRecordResponse> getRecordById(
            @PathVariable UUID recordId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);

        MockRecordResponse record =
                mockRecordService.getRecordById(userId, recordId);

        return ResponseEntity.ok(record);
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @PutMapping("/{org}/{project}/{schema}/records/{recordId}")
    public ResponseEntity<MockRecordResponse> updateRecord(
            @PathVariable UUID recordId,
            @Valid @RequestBody UpdateMockRecordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);

        MockRecordResponse updated =
                mockRecordService.updateRecord(userId, recordId, request);

        return ResponseEntity.ok(updated);
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @DeleteMapping("/{org}/{project}/{schema}/records/{recordId}")
    public ResponseEntity<Void> deleteRecord(
            @PathVariable UUID recordId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);

        mockRecordService.deleteRecord(userId, recordId);

        return ResponseEntity.noContent().build();
    }
}