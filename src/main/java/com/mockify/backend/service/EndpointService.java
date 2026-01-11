package com.mockify.backend.service;

import com.mockify.backend.model.*;
import java.util.UUID;

public interface EndpointService {

    // Create endpoints for new resources
    void createEndpoint(Organization organization);
    void createEndpoint(Project project);
    void createEndpoint(MockSchema schema);

    // Update endpoint when resource slug changes
    void updateEndpointSlug(UUID resourceId, String resourceType, String newSlug);

    // Delete endpoint when resource is deleted
    void deleteEndpoint(UUID resourceId, String resourceType);

    /**
     * Resolve organization from slug
     * Path: {org}
     * Example: "google"
     */
    UUID resolveOrganization(String orgSlug);

    /**
     * Resolve project from hierarchical path
     * Path: {org}/{project}
     * Example: "google/admin-backend"
     */
    UUID resolveProject(String orgSlug, String projectSlug);

    /**
     * Resolve schema from hierarchical path
     * Path: {org}/{project}/{schema}
     * Example: "google/admin-backend/users"
     */
    UUID resolveSchema(String orgSlug, String projectSlug, String schemaSlug);
}