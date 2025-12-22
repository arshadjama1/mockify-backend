package com.mockify.backend.service;

import com.mockify.backend.model.*;
import java.util.UUID;

public interface EndpointService {
    void createEndpoint(Organization organization);
    void createEndpoint(Project project);
    void createEndpoint(MockSchema schema);
    void updateEndpointSlug(UUID resourceId, String resourceType, String newSlug);
    void deleteEndpoint(UUID resourceId, String resourceType);

    UUID resolveOrganization(String orgSlug);

    UUID resolveProject(String orgSlug, String projectSlug);

    UUID resolveSchema(String projectSlug, String schemaSlug);
}