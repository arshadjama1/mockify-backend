package com.mockify.backend.repository;

import com.mockify.backend.model.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EndpointRepository extends JpaRepository<Endpoint, UUID> {

    /**
     * PRIMARY LOOKUP METHOD
     * Single query to resolve any endpoint by full hierarchical path
     * Examples:
     * - "google" → organization
     * - "google/admin-backend" → project
     * - "google/admin-backend/users" → schema
     */
    Optional<Endpoint> findByFullPath(String fullPath);

    /**
     * Check if path exists
     */
    boolean existsByFullPath(String fullPath);

    /**
     * Find endpoint by resource ID
     */
    Optional<Endpoint> findByOrganizationId(UUID organizationId);
    Optional<Endpoint> findByProjectId(UUID projectId);
    Optional<Endpoint> findBySchemaId(UUID schemaId);

    /**
     * Find all child endpoints of a parent
     * Useful for cascading updates when parent slug changes
     */
    List<Endpoint> findByParentEndpoint(Endpoint parent);

    /**
     * Find endpoints by resource type
     */
    List<Endpoint> findByResourceType(Endpoint.ResourceType resourceType);

    /**
     * Advanced query: Find endpoint with eager-loaded parent hierarchy
     * Useful when you need the full path context
     */
    @Query("""
        SELECT e FROM Endpoint e
        LEFT JOIN FETCH e.parentEndpoint p
        LEFT JOIN FETCH p.parentEndpoint
        WHERE e.fullPath = :fullPath
    """)
    Optional<Endpoint> findByFullPathWithHierarchy(@Param("fullPath") String fullPath);

    /**
     * Get all endpoints under a specific organization
     * Useful for bulk operations or admin views
     */
    @Query("""
        SELECT e FROM Endpoint e
        WHERE e.fullPath LIKE CONCAT(:orgPath, '%')
        ORDER BY e.fullPath
    """)
    List<Endpoint> findAllUnderOrganization(@Param("orgPath") String orgPath);

    /**
     * DEPRECATED - kept for backward compatibility during migration
     * Use findByFullPath instead
     */
    @Deprecated
    default Optional<Endpoint> findBySlug(String slug) {
        return findByFullPath(slug);
    }
}