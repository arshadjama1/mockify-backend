package com.mockify.backend.controller;

import com.mockify.backend.dto.request.schema.CreateMockSchemaRequest;
import com.mockify.backend.dto.request.schema.UpdateMockSchemaRequest;
import com.mockify.backend.dto.response.schema.MockSchemaDetailResponse;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.MockSchemaService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api")
@Tag(name = "Mock Schema")
public class MockSchemaController {

    private final MockSchemaService mockSchemaService;
    private final EndpointService endpointService;

    @PostMapping("/{org}/{project}/schemas")
    public ResponseEntity<MockSchemaResponse> createSchema(
            @PathVariable String org,
            @PathVariable String project,
            @RequestBody CreateMockSchemaRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("User {} creating schema '{}' under project {}", userId, request.getName(), project);

        // Resolve the project and extract projectId to create the schema under the correct project
        UUID projectId = endpointService.resolveProject(org, project);

        MockSchemaResponse response = mockSchemaService.createSchema(userId, projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get schema details
     */
    @GetMapping("/{org}/{project}/{schema}")
    public ResponseEntity<MockSchemaDetailResponse> getSchema(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID schemaId = endpointService.resolveSchema(org, project, schema);
        log.debug("User {} fetching schema {}", userId, schemaId);

        MockSchemaDetailResponse response = mockSchemaService.getSchemaById(userId, schemaId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update schema
     */
    @PutMapping("/{org}/{project}/{schema}")
    public ResponseEntity<MockSchemaResponse> updateSchema(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @RequestBody UpdateMockSchemaRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID schemaId = endpointService.resolveSchema(org, project, schema);
        log.info("User {} updating schema {}", userId, schemaId);

        MockSchemaResponse response = mockSchemaService.updateSchema(userId, schemaId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete schema
     */
    @DeleteMapping("/{org}/{project}/{schema}")
    public ResponseEntity<Void> deleteSchema(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID schemaId = endpointService.resolveSchema(org, project, schema);
        log.warn("User {} deleting schema {}", userId, schemaId);

        mockSchemaService.deleteSchema(userId, schemaId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all schemas under a project
     */
    @GetMapping("/{org}/{project}/schemas")
    public ResponseEntity<List<MockSchemaResponse>> getSchemasByProject(
            @PathVariable String org,
            @PathVariable String project,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID projectId = endpointService.resolveProject(org, project);
        log.debug("User {} fetching schemas under project {}", userId, projectId);

        List<MockSchemaResponse> schemas = mockSchemaService.getSchemasByProjectId(userId, projectId);
        return ResponseEntity.ok(schemas);
    }
}
