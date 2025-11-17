package com.mockify.backend.controller;

import com.mockify.backend.dto.request.record.CreateMockRecordRequest;
import com.mockify.backend.dto.request.record.UpdateMockRecordRequest;
import com.mockify.backend.dto.response.record.MockRecordResponse;
import com.mockify.backend.service.MockRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class MockRecordController {

    private final MockRecordService mockRecordService;

    // Create a new mock record
    @PostMapping("/schemas/{schemaId}/records")
    public ResponseEntity<MockRecordResponse> createRecord(
            @Valid @RequestBody CreateMockRecordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        log.info("User {} creating new mock record under schema {}", userId, request.getSchemaId());

        MockRecordResponse created = mockRecordService.createRecord(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Create multiple records in bulk
    @PostMapping("/schemas/{schemaId}/records/bulk")
    public ResponseEntity<List<MockRecordResponse>> createRecordsBulk(
            @Valid @RequestBody List<CreateMockRecordRequest> requests,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        log.info("User {} bulk creating {} records", userId, requests.size());

        List<MockRecordResponse> created = mockRecordService.createRecordsBulk(userId, requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Get a record by ID
    @GetMapping("/records/{recordId}")
    public ResponseEntity<MockRecordResponse> getRecordById(
            @PathVariable Long recordId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        log.debug("User {} fetching record with ID {}", userId, recordId);

        MockRecordResponse record = mockRecordService.getRecordById(userId, recordId);
        return ResponseEntity.ok(record);
    }

    // Get all records under a specific schema
    @GetMapping("/schemas/{schemaId}/records")
    public ResponseEntity<List<MockRecordResponse>> getRecordsBySchema(
            @PathVariable Long schemaId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        log.debug("User {} fetching all records under schema {}", userId, schemaId);

        List<MockRecordResponse> records = mockRecordService.getRecordsBySchemaId(userId, schemaId);
        return ResponseEntity.ok(records);
    }

    // Update an existing mock record
    @PutMapping("/records/{recordId}")
    public ResponseEntity<MockRecordResponse> updateRecord(
            @PathVariable Long recordId,
            @Valid @RequestBody UpdateMockRecordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        log.info("User {} updating record ID {}", userId, recordId);

        MockRecordResponse updated = mockRecordService.updateRecord(userId, recordId, request);
        return ResponseEntity.ok(updated);
    }

    // Delete a record by ID
    @DeleteMapping("/records/{recordId}")
    public ResponseEntity<Void> deleteRecord(
            @PathVariable Long recordId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        log.warn("User {} deleting record ID {}", userId, recordId);

        mockRecordService.deleteRecord(userId, recordId);
        return ResponseEntity.noContent().build();
    }
}
