package com.mockify.backend.service;

import com.mockify.backend.dto.request.organization.CreateOrganizationRequest;
import com.mockify.backend.dto.request.organization.UpdateOrganizationRequest;
import com.mockify.backend.dto.response.organization.OrganizationDetailResponse;
import com.mockify.backend.dto.response.organization.OrganizationResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

//Handles all CRUD operations related to organizations.
public interface OrganizationService {

    // Create new organization under current user
    OrganizationResponse createOrganization(UUID userId, CreateOrganizationRequest request);

    // Fetch organization details by ID
    OrganizationResponse getOrganizationById(UUID orgId);

    // Get organization details with its owner and projects.
    OrganizationDetailResponse getOrganizationDetail(UUID orgId, UUID userId);

    // Get all organizations owned by current user
    List<OrganizationResponse> getMyOrganizations(UUID userId);

    // Update organization name or details
    OrganizationResponse updateOrganization(UUID userId, UUID orgId, UpdateOrganizationRequest request);

    // Delete organization (and optionally its related projects)
    void deleteOrganization(UUID userId, UUID orgId);

    // Count total organizations in system
    long countOrganizations();
}
