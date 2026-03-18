package com.mockify.backend.repository;

import com.mockify.backend.dto.response.dashboard.*;
import com.mockify.backend.model.Organization;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.UUID;
public interface DashboardRepository extends Repository<Organization, UUID> {

    /*
     * USER-LEVEL STATS
     * Returns total counts of:
     * - Organizations owned by the user
     * - Projects inside those organizations
     * - Schemas inside those projects
     * - Records inside those schemas
     */
    @Query("""
        SELECT new com.mockify.backend.dto.response.dashboard.UserStats(
            COUNT(DISTINCT o.id),
            COUNT(DISTINCT p.id),
            COUNT(DISTINCT s.id),
            COUNT(DISTINCT r.id)
        )
        FROM Organization o
        LEFT JOIN o.projects p
        LEFT JOIN p.mockSchemas s
        LEFT JOIN s.mockRecords r
        WHERE o.owner.id = :userId
    """)
    UserStats userStats(@Param("userId") UUID userId);


    /*
     * ORGANIZATION-LEVEL STATS
     * Returns total counts of:
     * - Projects in the organization
     * - Schemas under those projects
     * - Records under those schemas
     */
    @Query("""
        SELECT new com.mockify.backend.dto.response.dashboard.OrganizationStats(
            COUNT(DISTINCT p.id),
            COUNT(DISTINCT s.id),
            COUNT(DISTINCT r.id)
        )
        FROM Project p
        LEFT JOIN p.mockSchemas s
        LEFT JOIN s.mockRecords r
        WHERE p.organization.id = :orgId
    """)
    OrganizationStats organizationStats(@Param("orgId") UUID orgId);


    /*
     * PROJECT-LEVEL STATS
     * Returns total counts of:
     * - Schemas in the project
     * - Records in those schemas
     * - Active records (not expired)
     * - Expired records
     */
    @Query("""
        SELECT new com.mockify.backend.dto.response.dashboard.ProjectStats(
            COUNT(DISTINCT s.id),
            COUNT(DISTINCT r.id),
            COUNT(CASE WHEN r.expiresAt > :now THEN 1 END),
            COUNT(CASE WHEN r.expiresAt <= :now THEN 1 END)
        )
        FROM MockSchema s
        LEFT JOIN s.mockRecords r
        WHERE s.project.id = :projectId
    """)
    ProjectStats projectStats(
            @Param("projectId") UUID projectId,
            @Param("now") LocalDateTime now
    );


    /*
     * SCHEMA-LEVEL STATS
     * Returns total counts of:
     * - All records in the schema
     * - Active records
     * - Expired records
     * - Records that are active but expiring soon
     */
    @Query("""
        SELECT new com.mockify.backend.dto.response.dashboard.SchemaStats(
            COUNT(r.id),
            COUNT(CASE WHEN r.expiresAt > :now THEN 1 END),
            COUNT(CASE WHEN r.expiresAt <= :now THEN 1 END),
            COUNT(CASE
                WHEN r.expiresAt > :now AND r.expiresAt <= :soon THEN 1
            END)
        )
        FROM MockRecord r
        WHERE r.mockSchema.id = :schemaId
    """)
    SchemaStats schemaStats(
            @Param("schemaId") UUID schemaId,
            @Param("now") LocalDateTime now,
            @Param("soon") LocalDateTime soon
    );


    /*
     * RECORD HEALTH STATS (USER-LEVEL)
     * Returns overall record health for a user:
     * - Total records
     * - Active records
     * - Expired records
     * - Records expiring soon
     */
    @Query("""
        SELECT new com.mockify.backend.dto.response.dashboard.RecordHealthStats(
            COUNT(r.id),
    
            COUNT(CASE
                WHEN r.expiresAt IS NULL OR r.expiresAt > :soon
                THEN 1
            END),
    
            COUNT(CASE
                WHEN r.expiresAt <= :now
                THEN 1
            END),
    
            COUNT(CASE
                WHEN r.expiresAt > :now AND r.expiresAt <= :soon
                THEN 1
            END)
        )
        FROM MockRecord r
        WHERE r.mockSchema.project.organization.owner.id = :userId
    """)
    RecordHealthStats recordHealthStats(
            @Param("userId") UUID userId,
            @Param("now") LocalDateTime now,
            @Param("soon") LocalDateTime soon
    );

}