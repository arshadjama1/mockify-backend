package com.mockify.backend.controller;

import com.mockify.backend.dto.request.schema.CreateMockSchemaRequest;
import com.mockify.backend.dto.request.schema.UpdateMockSchemaRequest;
import com.mockify.backend.dto.response.schema.MockSchemaDetailResponse;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.MockSchemaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Mock Schema")
public class MockSchemaController {

    private final MockSchemaService mockSchemaService;
    private final EndpointService endpointService;

    @PostMapping("/schemas")
    public ResponseEntity<MockSchemaResponse> createSchema(
            @RequestBody CreateMockSchemaRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        MockSchemaResponse response = mockSchemaService.createSchema(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projects/{projectId}/schemas")
    public ResponseEntity<List<MockSchemaResponse>> getSchemasByProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        List<MockSchemaResponse> schemas = mockSchemaService.getSchemasByProjectId(userId, projectId);
        return ResponseEntity.ok(schemas);
    }

    @GetMapping("/schemas/{schemaId}")
    public ResponseEntity<MockSchemaDetailResponse> getSchemaById(
            @PathVariable UUID schemaId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        MockSchemaDetailResponse response = mockSchemaService.getSchemaById(userId, schemaId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/schemas/{schemaId}")
    public ResponseEntity<MockSchemaResponse> updateSchema(
            @PathVariable UUID schemaId,
            @RequestBody UpdateMockSchemaRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        MockSchemaResponse response = mockSchemaService.updateSchema(userId, schemaId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/schemas/{schemaId}")
    public ResponseEntity<Void> deleteSchema(
            @PathVariable UUID schemaId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        mockSchemaService.deleteSchema(userId, schemaId);
        return ResponseEntity.noContent().build();
    }

    // SLUG-BASED ROUTES

    @GetMapping("/projects/slug/{projectSlug}/schemas")
    public ResponseEntity<List<MockSchemaResponse>> getSchemasByProjectSlug(
            @PathVariable String projectSlug,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID projectId = endpointService.resolveProjectId(projectSlug);

        List<MockSchemaResponse> schemas =
                mockSchemaService.getSchemasByProjectId(userId, projectId);

        return ResponseEntity.ok(schemas);
    }

    @GetMapping("/schemas/slug/{schemaSlug}")
    public ResponseEntity<MockSchemaDetailResponse> getSchemaBySlug(
            @PathVariable String schemaSlug,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID schemaId = endpointService.resolveSchemaId(schemaSlug);

        MockSchemaDetailResponse response =
                mockSchemaService.getSchemaById(userId, schemaId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/schemas/slug/{schemaSlug}")
    public ResponseEntity<MockSchemaResponse> updateSchemaBySlug(
            @PathVariable String schemaSlug,
            @RequestBody UpdateMockSchemaRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID schemaId = endpointService.resolveSchemaId(schemaSlug);

        MockSchemaResponse response =
                mockSchemaService.updateSchema(userId, schemaId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/schemas/slug/{schemaSlug}")
    public ResponseEntity<Void> deleteSchemaBySlug(
            @PathVariable String schemaSlug,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID schemaId = endpointService.resolveSchemaId(schemaSlug);

        mockSchemaService.deleteSchema(userId, schemaId);
        return ResponseEntity.noContent().build();
    }
}
