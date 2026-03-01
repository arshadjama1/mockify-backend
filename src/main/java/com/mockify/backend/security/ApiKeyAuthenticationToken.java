package com.mockify.backend.security;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

/**
 * Custom authentication token for API key-based authentication
 * Stores API key ID and scope information for authorization decisions
 */
@Getter
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final UUID apiKeyId;
    private final UUID organizationId;
    private final UUID projectId; // null for org-level keys

    public ApiKeyAuthenticationToken(
            UUID apiKeyId,
            UUID organizationId,
            UUID projectId,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiKeyId = apiKeyId;
        this.organizationId = organizationId;
        this.projectId = projectId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // API key is not stored in token for security
    }

    @Override
    public Object getPrincipal() {
        return apiKeyId.toString();
    }

    /**
     * Check if this API key is scoped to a specific project
     */
    public boolean isProjectScoped() {
        return projectId != null;
    }

    /**
     * Check if this API key has access to a specific project
     */
    public boolean hasProjectAccess(UUID requestedProjectId) {
        if (projectId == null) {
            return true; // Org-level key has access to all projects
        }
        return projectId.equals(requestedProjectId);
    }

    /**
     * Check if this API key has access to a specific organization
     */
    public boolean hasOrganizationAccess(UUID requestedOrgId) {
        return organizationId.equals(requestedOrgId);
    }
}