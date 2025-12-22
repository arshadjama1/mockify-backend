package com.mockify.backend.service.impl;

import com.mockify.backend.exception.DuplicateResourceException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.model.*;
import com.mockify.backend.repository.EndpointRepository;
import com.mockify.backend.service.EndpointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EndpointServiceImpl implements EndpointService {

    private final EndpointRepository endpointRepository;

    @Override
    @Transactional
    public void createEndpoint(Organization organization) {
        if (endpointRepository.existsBySlug(organization.getSlug())) {
            throw new DuplicateResourceException("Endpoint slug already exists: " + organization.getSlug());
        }

        Endpoint endpoint = new Endpoint();
        endpoint.setSlug(organization.getSlug());
        endpoint.setOrganization(organization);
        endpointRepository.save(endpoint);

        log.debug("Created endpoint for organization: {}", organization.getSlug());
    }

    @Override
    @Transactional
    public void createEndpoint(Project project) {
        Endpoint endpoint = new Endpoint();
        endpoint.setSlug(project.getSlug());
        endpoint.setProject(project);
        endpointRepository.save(endpoint);

        log.debug("Created endpoint for project: {}", project.getSlug());
    }

    @Override
    @Transactional
    public void createEndpoint(MockSchema schema) {
        Endpoint endpoint = new Endpoint();
        endpoint.setSlug(schema.getSlug());
        endpoint.setSchema(schema);
        endpointRepository.save(endpoint);

        log.debug("Created endpoint for schema: {}", schema.getSlug());
    }

    @Override
    @Transactional
    public void updateEndpointSlug(UUID resourceId, String resourceType, String newSlug) {
        Endpoint endpoint = switch (resourceType.toLowerCase()) {
            case "organization" -> endpointRepository.findByOrganizationId(resourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Endpoint not found"));
            case "project" -> endpointRepository.findByProjectId(resourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Endpoint not found"));
            case "schema" -> endpointRepository.findBySchemaId(resourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Endpoint not found"));
            default -> throw new IllegalArgumentException("Invalid resource type");
        };

        endpoint.setSlug(newSlug);
        endpointRepository.save(endpoint);

        log.debug("Updated endpoint slug to: {}", newSlug);
    }

    @Override
    @Transactional
    public void deleteEndpoint(UUID resourceId, String resourceType) {
        Endpoint endpoint = switch (resourceType.toLowerCase()) {
            case "organization" -> endpointRepository.findByOrganizationId(resourceId)
                    .orElse(null);
            case "project" -> endpointRepository.findByProjectId(resourceId)
                    .orElse(null);
            case "schema" -> endpointRepository.findBySchemaId(resourceId)
                    .orElse(null);
            default -> null;
        };

        if (endpoint != null) {
            endpointRepository.delete(endpoint);
            log.debug("Deleted endpoint: {}", endpoint.getSlug());
        }
    }

    @Override
    public UUID resolveOrganization(String orgSlug) {
        Endpoint org = endpointRepository.findBySlug(orgSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgSlug));

        return org.getOrganization().getId();
    }

    @Override
    @Transactional(readOnly = true)
    public UUID resolveProject(String orgSlug, String projectSlug) {

        UUID orgId = resolveOrganization(orgSlug);

        Endpoint endpoint = endpointRepository
                .findByOrganizationIdAndSlug(orgId, projectSlug)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project not found: " + projectSlug + " under org " + orgSlug
                        )
                );

        if (endpoint.getProject() == null) {
            throw new ResourceNotFoundException("Slug does not point to a project");
        }

        return endpoint.getProject().getId();
    }

    @Override
    @Transactional(readOnly = true)
    public UUID resolveSchema(String projectSlug, String schemaSlug) {

        UUID projectId = resolveProject(projectSlug, schemaSlug);

        Endpoint endpoint = endpointRepository
                .findByProjectIdAndSlug(projectId, schemaSlug)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Schema not found: " + schemaSlug + " under project " + projectSlug
                        )
                );

        if (endpoint.getSchema() == null) {
            throw new ResourceNotFoundException("Slug does not point to a schema");
        }

        return endpoint.getSchema().getId();
    }
}