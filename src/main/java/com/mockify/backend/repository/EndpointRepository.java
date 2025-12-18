package com.mockify.backend.repository;

import com.mockify.backend.model.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EndpointRepository extends JpaRepository<Endpoint, UUID> {
    Optional<Endpoint> findBySlug(String slug);
    Optional<Endpoint> findByOrganizationId(UUID organizationId);
    Optional<Endpoint> findByProjectId(UUID projectId);
    Optional<Endpoint> findBySchemaId(UUID schemaId);
    boolean existsBySlug(String slug);
}