package com.mockify.backend.service;

import com.mockify.backend.dto.request.project.CreateProjectRequest;
import com.mockify.backend.dto.request.project.UpdateProjectRequest;
import com.mockify.backend.dto.response.project.ProjectDetailResponse;
import com.mockify.backend.dto.response.project.ProjectResponse;

import java.util.List;
import java.util.UUID;

public interface ProjectService {

    // Create new project under an organization
    ProjectResponse createProject(UUID userId, CreateProjectRequest request);

    // Get all projects under a specific organization
    List<ProjectResponse> getProjectsByOrganizationId(UUID userId, UUID organizationId);

    // Fetch project details by ID
    ProjectDetailResponse getProjectById(UUID userId, UUID projectId);

    // Update project name or info
    ProjectResponse updateProject(UUID userId, UUID projectId, UpdateProjectRequest request);

    // Delete project by ID
    void deleteProject(UUID userId, UUID projectId);

    // Count total projects
    long countProjects();
}
