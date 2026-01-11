package com.mockify.backend.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "endpoints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Endpoint {
    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Full hierarchical path for URL routing
     * Examples:
     * - Organization: "google"
     * - Project: "google/admin-backend"
     * - Schema: "google/admin-backend/users"
     */
    @Column(name = "full_path", nullable = false, unique = true, length = 500)
    private String fullPath;

    /**
     * Individual slug for this resource
     * Examples: "google", "admin-backend", "users"
     */
    @Column(nullable = false, length = 255)
    private String slug;

    /**
     * Resource type for fast filtering without checking FKs
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 20)
    private ResourceType resourceType;

    /**
     * Parent endpoint in the hierarchy
     * - Organizations have NULL parent
     * - Projects have organization as parent
     * - Schemas have project as parent
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_endpoint_id")
    private Endpoint parentEndpoint;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Resource Foreign Keys (exactly ONE will be non-null)
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schema_id")
    private MockSchema schema;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        validateFk();
        validateResourceType();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateFk();
        validateResourceType();
    }

    /**
     * Validates that exactly one FK is set
     */
    private void validateFk() {
        int count = 0;
        if (organization != null) count++;
        if (project != null) count++;
        if (schema != null) count++;

        if (count != 1) {
            throw new IllegalStateException(
                    "Endpoint must reference exactly one entity (organization, project, or schema)"
            );
        }
    }

    /**
     * Validates that resource type matches the FK
     */
    private void validateResourceType() {
        if (resourceType == null) {
            throw new IllegalStateException("Resource type cannot be null");
        }

        boolean valid = switch (resourceType) {
            case ORGANIZATION -> organization != null;
            case PROJECT -> project != null;
            case SCHEMA -> schema != null;
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Resource type " + resourceType + " does not match foreign key"
            );
        }
    }

    /**
     * Enum for resource types
     */
    public enum ResourceType {
        ORGANIZATION,
        PROJECT,
        SCHEMA
    }

    /**
     * Helper method to get the resource ID regardless of type
     */
    public UUID getResourceId() {
        return switch (resourceType) {
            case ORGANIZATION -> organization != null ? organization.getId() : null;
            case PROJECT -> project != null ? project.getId() : null;
            case SCHEMA -> schema != null ? schema.getId() : null;
        };
    }

    /**
     * Helper method to build full path from parent + slug
     */
    public static String buildFullPath(Endpoint parent, String slug) {
        if (parent == null) {
            return slug;
        }
        return parent.getFullPath() + "/" + slug;
    }
}