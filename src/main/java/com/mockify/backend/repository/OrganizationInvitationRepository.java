package com.mockify.backend.repository;

import com.mockify.backend.model.OrganizationInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationInvitationRepository extends JpaRepository<OrganizationInvitation, UUID> {

    @Query("""
        SELECT i FROM OrganizationInvitation i
        WHERE i.organization.id = :orgId
          AND i.acceptedAt IS NULL
          AND i.cancelledAt IS NULL
    """)
    List<OrganizationInvitation> findPendingInvitationsByOrganizationId(@Param("orgId") UUID orgId);

    @Query("""
        SELECT i FROM OrganizationInvitation i
        WHERE i.organization.id = :orgId
          AND i.email = :email
          AND i.acceptedAt IS NULL
          AND i.cancelledAt IS NULL
    """)
    Optional<OrganizationInvitation> findPendingInvitationByEmail(
            @Param("orgId") UUID orgId,
            @Param("email") String email
    );

    @Query("""
        SELECT i FROM OrganizationInvitation i
        WHERE i.tokenHash = :tokenHash
          AND i.acceptedAt IS NULL
          AND i.cancelledAt IS NULL
          AND i.expiresAt > :now
    """)
    Optional<OrganizationInvitation> findPendingInvitationByTokenHash(
            @Param("tokenHash") String tokenHash,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM OrganizationInvitation i WHERE i.expiresAt < :now AND i.acceptedAt IS NULL AND i.cancelledAt IS NULL")
    int deleteExpiredInvitations(@Param("now") LocalDateTime now);
}