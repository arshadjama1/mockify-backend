package com.mockify.backend.service.impl;

import com.mockify.backend.dto.request.apikey.CreateApiKeyRequest;
import com.mockify.backend.dto.request.apikey.UpdateApiKeyRequest;
import com.mockify.backend.dto.response.apikey.ApiKeyResponse;
import com.mockify.backend.dto.response.apikey.ApiKeyStats;
import com.mockify.backend.dto.response.apikey.CreateApiKeyResult;
import com.mockify.backend.exception.AccessDeniedException;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.model.*;
import com.mockify.backend.model.ApiKeyPermission.ApiPermission;
import com.mockify.backend.model.ApiKeyPermission.ApiResourceType;
import com.mockify.backend.repository.*;
import com.mockify.backend.security.ApiKeyCryptoService;
import com.mockify.backend.service.AccessControlService;
import com.mockify.backend.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyPermissionRepository permissionRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ApiKeyCryptoService cryptoService;
    private final AccessControlService accessControlService;

    @Value("${app.api-key.secret}")
    private String globalSecret;

    @Value("${app.api-key.max-per-organization:50}")
    private int maxKeysPerOrganization;

    @Override
    @Transactional
    public CreateApiKeyResult createApiKey(
            UUID userId,
            UUID organizationId,
            CreateApiKeyRequest request) {

        log.info("User {} creating API key for organization {}", userId, organizationId);

        // Validate user has access to organization
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        accessControlService.checkOrganizationAccess(userId, organization, "Organization");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check organization key limit
        long activeKeys = apiKeyRepository.countActiveKeysByOrganization(
                organizationId,
                LocalDateTime.now()
        );

        if (activeKeys >= maxKeysPerOrganization) {
            throw new BadRequestException(
                    "Maximum number of API keys reached for this organization"
            );
        }

        // Validate project scope if specified
        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

            if (!project.getOrganization().getId().equals(organizationId)) {
                throw new BadRequestException("Project does not belong to this organization");
            }
        }

        // Validate expiration date
        if (request.getExpiresAt() != null &&
                request.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Expiration date must be in the future");
        }

        // Generate API key
        String rawApiKey = cryptoService.generateApiKey(false);
        String keyPrefix = cryptoService.extractKeyPrefix(rawApiKey);

        // Hash key with organization-specific secret
        String orgSecret = cryptoService.generateOrgSecret(
                organizationId.toString(),
                globalSecret
        );
        String keyHash = cryptoService.hashApiKey(rawApiKey, orgSecret);

        // Check for hash collision (extremely unlikely but must handle)
        if (apiKeyRepository.existsByOrganizationIdAndKeyHash(organizationId, keyHash)) {
            log.error("API key hash collision detected! Regenerating...");
            return createApiKey(userId, organizationId, request); // Retry
        }

        int rateLimitPerMin = request.getRateLimitPerMinute() != null
                ? request.getRateLimitPerMinute()
                : 1000;

        // Create API key entity
        ApiKey apiKey = ApiKey.builder()
                .name(request.getName())
                .description(request.getDescription())
                .keyPrefix(keyPrefix)
                .keyHash(keyHash)
                .organization(organization)
                .createdBy(user)
                .project(project)
                .isActive(true)
                .expiresAt(request.getExpiresAt())
                .rateLimitPerMinute(rateLimitPerMin)
                .build();

        // Create permissions
        for (var permReq : request.getPermissions()) {

            validatePermission(permReq, organization, project);

            ApiKeyPermission permission = ApiKeyPermission.builder()
                    .permission(permReq.getPermission())
                    .resourceType(permReq.getResourceType())
                    .resourceId(permReq.getResourceId())
                    .build();

            apiKey.addPermission(permission);
        }

        apiKey = apiKeyRepository.save(apiKey);

        log.info("API key created: id={}, org={}, project={}",
                apiKey.getId(), organizationId, request.getProjectId());

        return CreateApiKeyResult.builder()
                .apiKey(rawApiKey)
                .keyInfo(toResponse(apiKey))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listOrganizationKeys(UUID userId, UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        accessControlService.checkOrganizationAccess(userId, organization, "Organization");

        List<ApiKey> keys = apiKeyRepository.findByOrganizationId(organizationId);
        return keys.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listProjectKeys(UUID userId, UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        accessControlService.checkOrganizationAccess(
                userId,
                project.getOrganization(),
                "Project"
        );

        List<ApiKey> keys = apiKeyRepository.findByProjectId(projectId);
        return keys.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ApiKeyResponse getApiKeyById(UUID userId, UUID keyId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found"));

        accessControlService.checkOrganizationAccess(
                userId,
                apiKey.getOrganization(),
                "API Key"
        );

        return toResponse(apiKey);
    }

    @Override
    @Transactional
    public ApiKeyResponse updateApiKey(UUID userId, UUID keyId, UpdateApiKeyRequest request) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found"));

        accessControlService.checkOrganizationAccess(
                userId,
                apiKey.getOrganization(),
                "API Key"
        );

        // Update fields
        if (request.getName() != null) {
            apiKey.setName(request.getName());
        }

        if (request.getDescription() != null) {
            apiKey.setDescription(request.getDescription());
        }

        if (request.getIsActive() != null) {
            apiKey.setActive(request.getIsActive());
        }

        if (request.getExpiresAt() != null) {
            if (request.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Expiration date must be in the future");
            }
            apiKey.setExpiresAt(request.getExpiresAt());
        }

        if (request.getRateLimitPerMinute() != null) {
            apiKey.setRateLimitPerMinute(request.getRateLimitPerMinute());
        }

        apiKey = apiKeyRepository.save(apiKey);
        log.info("API key updated: id={}", keyId);

        return toResponse(apiKey);
    }

    @Override
    @Transactional
    public void revokeApiKey(UUID userId, UUID keyId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found"));

        accessControlService.checkOrganizationAccess(
                userId,
                apiKey.getOrganization(),
                "API Key"
        );

        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);

        log.warn("API key revoked: id={} by user={}", keyId, userId);
    }

    @Override
    @Transactional
    public void deleteApiKey(UUID userId, UUID keyId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found"));

        accessControlService.checkOrganizationAccess(
                userId,
                apiKey.getOrganization(),
                "API Key"
        );

        apiKeyRepository.delete(apiKey);
        log.warn("API key deleted: id={} by user={}", keyId, userId);
    }

    @Override
    @Transactional
    public CreateApiKeyResult rotateApiKey(UUID userId, UUID keyId) {
        ApiKey oldKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found"));

        accessControlService.checkOrganizationAccess(
                userId,
                oldKey.getOrganization(),
                "API Key"
        );

        // Create request from old key
        CreateApiKeyRequest request = new CreateApiKeyRequest();
        request.setName(oldKey.getName() + " (Rotated)");
        request.setDescription(oldKey.getDescription());
        request.setProjectId(oldKey.getProject() != null ? oldKey.getProject().getId() : null);
        request.setExpiresAt(oldKey.getExpiresAt());
        request.setRateLimitPerMinute(oldKey.getRateLimitPerMinute());

        // Copy permissions
        List<CreateApiKeyRequest.PermissionRequest> permissions = new ArrayList<>();
        for (ApiKeyPermission perm : oldKey.getPermissions()) {
            permissions.add(new CreateApiKeyRequest.PermissionRequest(
                    perm.getPermission(),
                    perm.getResourceType(),
                    perm.getResourceId()
            ));
        }
        request.setPermissions(permissions);

        // Create new key
        CreateApiKeyResult result = createApiKey(
                userId,
                oldKey.getOrganization().getId(),
                request
        );

        // Revoke old key
        oldKey.setActive(false);
        apiKeyRepository.save(oldKey);

        log.info("API key rotated: old={}, new={}", keyId, result.getKeyInfo().getId());

        return result;
    }

    // Helper methods

    private void validatePermission(
            CreateApiKeyRequest.PermissionRequest permReq,
            Organization organization,
            Project project) {

        // If resource ID is specified, validate it exists and belongs to org/project
        if (permReq.getResourceId() != null) {
            // Add validation logic based on resource type
            // This is simplified - you'd need to check each resource type
            log.debug("Validating permission for resource: {} - {}",
                    permReq.getResourceType(), permReq.getResourceId());
        }
    }

    private ApiKeyResponse toResponse(ApiKey apiKey) {
        ApiKeyResponse response = new ApiKeyResponse();
        response.setId(apiKey.getId());
        response.setName(apiKey.getName());
        response.setDescription(apiKey.getDescription());
        response.setKeyPrefix(apiKey.getKeyPrefix() + "****");
        response.setOrganizationId(apiKey.getOrganization().getId());
        response.setOrganizationName(apiKey.getOrganization().getName());

        if (apiKey.getProject() != null) {
            response.setProjectId(apiKey.getProject().getId());
            response.setProjectName(apiKey.getProject().getName());
        }

        response.setCreatedBy(apiKey.getCreatedBy().getId());
        response.setCreatedByName(apiKey.getCreatedBy().getName());
        response.setActive(apiKey.isActive());
        response.setExpiresAt(apiKey.getExpiresAt());
        response.setLastUsedAt(apiKey.getLastUsedAt());
        response.setCreatedAt(apiKey.getCreatedAt());
        response.setUpdatedAt(apiKey.getUpdatedAt());
        response.setRateLimitPerMinute(apiKey.getRateLimitPerMinute());

        // Map permissions
        List<ApiKeyResponse.PermissionResponse> permissions = apiKey.getPermissions().stream()
                .map(p -> ApiKeyResponse.PermissionResponse.builder()
                        .id(p.getId())
                        .permission(p.getPermission().name())
                        .resourceType(p.getResourceType().name())
                        .resourceId(p.getResourceId())
                        .build())
                .collect(Collectors.toList());

        response.setPermissions(permissions);

        return response;
    }
}