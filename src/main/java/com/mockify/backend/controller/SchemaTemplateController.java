package com.mockify.backend.controller;

import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import com.mockify.backend.dto.response.schema.SchemaTemplateResponse;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.SchemaTemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schema-templates")
@RequiredArgsConstructor
@Tag(name = "Mock Schema Templates")
public class SchemaTemplateController {

    private final EndpointService endpointService;

    private final SchemaTemplateService schemaTemplateService;

    // Public, built-in templates that are shared across all organizations.
    @GetMapping("/system")
    public ResponseEntity<List<SchemaTemplateResponse>> getSystemTemplates() {

        List<SchemaTemplateResponse> templates =
                schemaTemplateService.getSystemTemplates();

        return ResponseEntity.ok(templates);
    }

    // Apply a system template to a specific project using human-readable slugs.
    // Slugs are used instead of IDs to keep URLs stable and user-friendly.
    @PostMapping("/{org}/{project}/{templateSlug}")
    public ResponseEntity<MockSchemaResponse> applyTemplate(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String templateSlug,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        // Username stores the UUID in our auth setup.
        // Converting here avoids leaking auth-layer assumptions into service logic.
        UUID userId = UUID.fromString(userDetails.getUsername());

        // Resolve project ID from (orgSlug + projectSlug) instead of trusting client-provided IDs.
        UUID projectId = endpointService.resolveProject(org, project);

        MockSchemaResponse response =
                schemaTemplateService.applyTemplateToProject(
                        userId,
                        projectId,
                        templateSlug
                );

        // applying a template results in a new schema being created for the project.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
