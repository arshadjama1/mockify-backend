package com.mockify.backend.controller;

import com.mockify.backend.dto.request.record.CreateMockRecordRequest;
import com.mockify.backend.dto.request.record.UpdateMockRecordRequest;
import com.mockify.backend.dto.response.record.MockRecordResponse;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.MockRecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Mock Record")
public class MockRecordController {

    private final MockRecordService mockRecordService;
    private final EndpointService endpointService;

    // Create a new mock record
    @PostMapping("/schemas/{schemaId}/records")
    public ResponseEntity<MockRecordResponse> createRecord(
            @PathVariable UUID schemaId,
            @Valid @RequestBody CreateMockRecordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("User {} creating new mock record under schema {}", userId, schemaId);

        MockRecordResponse created = mockRecordService.createRecord(userId, schemaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Create multiple records in bulk
    @PostMapping("/schemas/{schemaId}/records/bulk")
    public ResponseEntity<List<MockRecordResponse>> createRecordsBulk(
            @PathVariable UUID schemaId,
            @Valid @RequestBody List<CreateMockRecordRequest> requests,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("User {} bulk creating {} records", userId, requests.size());

        List<MockRecordResponse> created = mockRecordService.createRecordsBulk(userId, schemaId, requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Get a record by ID
    @GetMapping("/records/{recordId}")
    public ResponseEntity<MockRecordResponse> getRecordById(
            @PathVariable UUID recordId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.debug("User {} fetching record with ID {}", userId, recordId);

        MockRecordResponse record = mockRecordService.getRecordById(userId, recordId);
        return ResponseEntity.ok(record);
    }

    // Get all records under a specific schema
    @GetMapping("/schemas/{schemaId}/records")
    public ResponseEntity<List<MockRecordResponse>> getRecordsBySchema(
            @PathVariable UUID schemaId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.debug("User {} fetching all records under schema {}", userId, schemaId);

        List<MockRecordResponse> records = mockRecordService.getRecordsBySchemaId(userId, schemaId);
        return ResponseEntity.ok(records);
    }

    // Update an existing mock record
    @PutMapping("/records/{recordId}")
    public ResponseEntity<MockRecordResponse> updateRecord(
            @PathVariable UUID recordId,
            @Valid @RequestBody UpdateMockRecordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("User {} updating record ID {}", userId, recordId);

        MockRecordResponse updated = mockRecordService.updateRecord(userId, recordId, request);
        return ResponseEntity.ok(updated);
    }

    // Delete a record by ID
    @DeleteMapping("/records/{recordId}")
    public ResponseEntity<Void> deleteRecord(
            @PathVariable UUID recordId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.warn("User {} deleting record ID {}", userId, recordId);

        mockRecordService.deleteRecord(userId, recordId);
        return ResponseEntity.noContent().build();
    }

    // SLUG-BASED ROUTES

    @PostMapping("/schemas/slug/{schemaSlug}/records")
    public ResponseEntity<MockRecordResponse> createRecordBySlug(
            @PathVariable String schemaSlug,
            @Valid @RequestBody CreateMockRecordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID schemaId = endpointService.resolveSchemaId(schemaSlug);

        log.info("User {} creating record under schema {}", userId, schemaId);

        MockRecordResponse created =
                mockRecordService.createRecord(userId, schemaId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/schemas/slug/{schemaSlug}/records/bulk")
    public ResponseEntity<List<MockRecordResponse>> createRecordsBulkBySlug(
            @PathVariable String schemaSlug,
            @Valid @RequestBody List<CreateMockRecordRequest> requests,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID schemaId = endpointService.resolveSchemaId(schemaSlug);

        log.info("User {} bulk creating {} records under schema {}",
                userId, requests.size(), schemaId);

        List<MockRecordResponse> created =
                mockRecordService.createRecordsBulk(userId, schemaId, requests);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/schemas/slug/{schemaSlug}/records")
    public ResponseEntity<List<MockRecordResponse>> getRecordsBySchemaSlug(
            @PathVariable String schemaSlug,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID schemaId = endpointService.resolveSchemaId(schemaSlug);

        log.debug("User {} fetching records under schema {}", userId, schemaId);

        List<MockRecordResponse> records =
                mockRecordService.getRecordsBySchemaId(userId, schemaId);

        return ResponseEntity.ok(records);
    }

    @GetMapping("/schemas/slug/{schemaSlug}/records/{recordId}")
    public ResponseEntity<MockRecordResponse> getRecordBySlug(
            @PathVariable String schemaSlug,
            @PathVariable UUID recordId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID schemaId = endpointService.resolveSchemaId(schemaSlug);

        log.debug("User {} fetching record {} under schema {}", userId, recordId, schemaId);

        MockRecordResponse record =
                mockRecordService.getRecordById(userId, recordId);

        return ResponseEntity.ok(record);
    }

    @PutMapping("/schemas/slug/{schemaSlug}/records/{recordId}")
    public ResponseEntity<MockRecordResponse> updateRecordBySlug(
            @PathVariable String schemaSlug,
            @PathVariable UUID recordId,
            @Valid @RequestBody UpdateMockRecordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID schemaId = endpointService.resolveSchemaId(schemaSlug);

        log.info("User {} updating record {} under schema {}", userId, recordId, schemaId);

        MockRecordResponse updated =
                mockRecordService.updateRecord(userId, recordId, request);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/schemas/slug/{schemaSlug}/records/{recordId}")
    public ResponseEntity<Void> deleteRecordBySlug(
            @PathVariable String schemaSlug,
            @PathVariable UUID recordId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID schemaId = endpointService.resolveSchemaId(schemaSlug);

        log.warn("User {} deleting record {} under schema {}", userId, recordId, schemaId);

        mockRecordService.deleteRecord(userId, recordId);
        return ResponseEntity.noContent().build();
    }
}
