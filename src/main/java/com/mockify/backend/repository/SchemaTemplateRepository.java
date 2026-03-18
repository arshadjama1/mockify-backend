package com.mockify.backend.repository;

import com.mockify.backend.model.SchemaTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchemaTemplateRepository extends JpaRepository<SchemaTemplate, UUID> {

    List<SchemaTemplate> findByOrganizationIdOrSystemTemplateTrue(UUID orgId);

    List<SchemaTemplate> findBySystemTemplateTrue();   // ✅ NEW

    Optional<SchemaTemplate> findBySlugAndOrganizationId(String slug, UUID orgId);

    Optional<SchemaTemplate> findBySlugAndSystemTemplateTrue(String slug);

    boolean existsBySlugAndOrganizationId(String slug, UUID orgId);
}
