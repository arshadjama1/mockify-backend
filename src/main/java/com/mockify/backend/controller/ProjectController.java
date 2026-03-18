package com.mockify.backend.controller;

import com.mockify.backend.dto.request.project.CreateProjectRequest;
import com.mockify.backend.dto.request.project.UpdateProjectRequest;
import com.mockify.backend.dto.response.page.PageResponse;
import com.mockify.backend.dto.response.project.ProjectDetailResponse;
import com.mockify.backend.dto.response.project.ProjectResponse;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.ProjectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
@Tag(name = "Project")
public class ProjectController {

    private final ProjectService projectService;
    private final EndpointService endpointService;

    //  Create a new project under an organization
    @PostMapping("/{org}/projects")
    public ResponseEntity<ProjectResponse> createProject(
            @PathVariable String org,
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("User {} creating new project '{}' under organization {}", userId, request.getName(), org);

        // Resolve the organization and extract orgId to create the project under the correct org
        UUID orgId = endpointService.resolveOrganization(org);

        ProjectResponse created = projectService.createProject(userId, orgId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    //  Get a single project details
    @GetMapping("/{org}/{project}")
    public ResponseEntity<ProjectDetailResponse> getProject(
            @PathVariable String org,
            @PathVariable String project,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID projectId = endpointService.resolveProject(org, project);

        log.debug("User {} fetching project details for ID {}", userId, projectId);

        ProjectDetailResponse response = projectService.getProjectById(userId, projectId);
        return ResponseEntity.ok(response);
    }

    // Update an existing project
    @PutMapping("/{org}/{project}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable String org,
            @PathVariable String project,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID projectId = endpointService.resolveProject(org, project);
        log.info("User {} updating project {}", userId, projectId);

        ProjectResponse updated = projectService.updateProject(userId, projectId, request);
        return ResponseEntity.ok(updated);
    }

    //  Delete a project
    @DeleteMapping("/{org}/{project}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable String org,
            @PathVariable String project,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID projectId = endpointService.resolveProject(org, project);
        log.warn("User {} deleting project ID {}", userId, projectId);

        projectService.deleteProject(userId, projectId);
        return ResponseEntity.noContent().build();
    }

    //  Get all projects under a specific organization
    @GetMapping("/{org}/projects")
    public ResponseEntity<PageResponse<ProjectResponse>> getProjectsByOrganization(
            @PathVariable String org,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID organizationId = endpointService.resolveOrganization(org);

        Page<ProjectResponse> page =
                projectService.getProjectsByOrganizationId(userId, organizationId, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }
}
