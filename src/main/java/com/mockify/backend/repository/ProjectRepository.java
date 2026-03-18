package com.mockify.backend.repository;

import com.mockify.backend.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    // Find all projects under an organization
    Page<Project> findByOrganizationId(UUID organizationId, Pageable pageable);

    // Find project by name and organization
    Project findByNameAndOrganizationId(String name, UUID organizationId);

    // Delete all projects under an organization
    void deleteByOrganizationId(UUID organizationId);

    // Count all projects
    long count();

    Optional<Project> findBySlugAndOrganizationId(String slug, UUID organizationId);

    boolean existsBySlugAndOrganizationId(String slug, UUID organizationId);
}