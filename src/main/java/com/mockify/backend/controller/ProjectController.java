package com.mockify.backend.controller;

import com.mockify.backend.dto.request.project.CreateProjectRequest;
import com.mockify.backend.dto.request.project.UpdateProjectRequest;
import com.mockify.backend.dto.response.project.ProjectDetailResponse;
import com.mockify.backend.dto.response.project.ProjectResponse;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.ProjectService;
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
@Tag(name = "Project")
public class ProjectController {

    private final ProjectService projectService;
    private final EndpointService endpointService;

    //  Create a new project under an organization
    @PostMapping("/projects")
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("User {} creating new project '{}' under organization {}", userId, request.getName(), request.getOrganizationId());

        ProjectResponse created = projectService.createProject(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    //  Get a project by ID
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ProjectDetailResponse> getProjectById(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.debug("User {} fetching project details for ID {}", userId, projectId);

        ProjectDetailResponse project = projectService.getProjectById(userId, projectId);
        return ResponseEntity.ok(project);
    }

    //  Get all projects under a specific organization
    @GetMapping("/organizations/{organizationId}/projects")
    public ResponseEntity<List<ProjectResponse>> getProjectsByOrganization(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.debug("User {} fetching all projects under organization {}", userId, organizationId);

        List<ProjectResponse> projects = projectService.getProjectsByOrganizationId(userId, organizationId);
        return ResponseEntity.ok(projects);
    }

    // Update an existing project
    @PutMapping("/projects/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("User {} updating project ID {} with new data: {}", userId, projectId, request);

        ProjectResponse updated = projectService.updateProject(userId, projectId, request);
        return ResponseEntity.ok(updated);
    }

    //  Delete a project
    @DeleteMapping("/projects/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.warn("User {} deleting project ID {}", userId, projectId);

        projectService.deleteProject(userId, projectId);
        return ResponseEntity.noContent().build();
    }

    // Count total projects
    @GetMapping("/projects/count")
    public ResponseEntity<Long> countProjects() {
        long count = projectService.countProjects();
        log.info("Total number of projects in the system: {}", count);
        return ResponseEntity.ok(count);
    }

    // SLUG-BASED ROUTES

    @GetMapping("/projects/slug/{projectSlug}")
    public ResponseEntity<ProjectDetailResponse> getProjectBySlug(
            @PathVariable String projectSlug,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID projectId = endpointService.resolveProjectId(projectSlug);

        log.debug("User {} fetching project {}", userId, projectId);

        ProjectDetailResponse project =
                projectService.getProjectById(userId, projectId);

        return ResponseEntity.ok(project);
    }

    @GetMapping("/organizations/slug/{orgSlug}/projects")
    public ResponseEntity<List<ProjectResponse>> getProjectsByOrganizationSlug(
            @PathVariable String orgSlug,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID organizationId = endpointService.resolveOrganizationId(orgSlug);

        log.debug("User {} fetching projects under organization {}", userId, organizationId);

        List<ProjectResponse> projects =
                projectService.getProjectsByOrganizationId(userId, organizationId);

        return ResponseEntity.ok(projects);
    }

    @PutMapping("/projects/slug/{projectSlug}")
    public ResponseEntity<ProjectResponse> updateProjectBySlug(
            @PathVariable String projectSlug,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID projectId = endpointService.resolveProjectId(projectSlug);

        log.info("User {} updating project {}", userId, projectId);

        ProjectResponse updated =
                projectService.updateProject(userId, projectId, request);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/projects/slug/{projectSlug}")
    public ResponseEntity<Void> deleteProjectBySlug(
            @PathVariable String projectSlug,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID projectId = endpointService.resolveProjectId(projectSlug);

        log.warn("User {} deleting project {}", userId, projectId);

        projectService.deleteProject(userId, projectId);
        return ResponseEntity.noContent().build();
    }
}
