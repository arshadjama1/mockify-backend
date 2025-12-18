package com.mockify.backend.service;

import com.mockify.backend.model.*;
import java.util.UUID;

public interface EndpointService {
    void createEndpoint(Organization organization);
    void createEndpoint(Project project);
    void createEndpoint(MockSchema schema);
    void updateEndpointSlug(UUID resourceId, String resourceType, String newSlug);
    void deleteEndpoint(UUID resourceId, String resourceType);
    UUID resolveOrganizationId(String slug);
    UUID resolveProjectId(String slug);
    UUID resolveSchemaId(String slug);
}