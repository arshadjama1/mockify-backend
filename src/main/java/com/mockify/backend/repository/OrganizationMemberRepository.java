package com.mockify.backend.repository;

import com.mockify.backend.common.enums.MemberRole;
import com.mockify.backend.model.Organization;
import com.mockify.backend.model.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID orgId, UUID userId);

    List<OrganizationMember> findByOrganizationId(UUID orgId);

    boolean existsByOrganizationIdAndUserId(UUID orgId, UUID userId);

    @Query("SELECT m.role FROM OrganizationMember m WHERE m.organization.id = :orgId AND m.user.id = :userId")
    Optional<MemberRole> findRoleByOrganizationIdAndUserId(@Param("orgId") UUID orgId, @Param("userId") UUID userId);

    @Query("""
        SELECT m.organization FROM OrganizationMember m
        WHERE m.user.id = :userId
        ORDER BY m.organization.createdAt DESC
    """)
    List<Organization> findOrganizationsByMemberId(@Param("userId") UUID userId);

    long countByOrganizationId(UUID orgId);

    void deleteByOrganizationIdAndUserId(UUID orgId, UUID userId);
}