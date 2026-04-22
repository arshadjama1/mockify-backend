package com.mockify.backend.service;

import com.mockify.backend.dto.request.member.*;
import com.mockify.backend.dto.response.member.*;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for managing organization membership and invitations.
 *
 * <p>This service encapsulates:
 * - Member lifecycle (add, remove, role changes, leave)
 * - Invitation lifecycle (invite, accept, cancel, list)
 * - Ownership management (transfer)
 *
 * <p>All methods are expected to enforce:
 * - Role-based authorization using {@link com.mockify.backend.common.enums.MemberRole}
 * - Organization scoping (actor must belong to the org where required)
 * - Invariants such as:
 *   - Single OWNER per organization
 *   - No duplicate membership
 *   - Invitation validity (not expired / already used)
 *
 * <p>Implementations should be transactional where mutations occur.
 */
public interface OrganizationMemberService {

    /**
     * Invite a user (by email) to join an organization with a specific role.
     *
     * <p>Rules:
     * - Actor must have permission to invite (ADMIN+ typically)
     * - Actor cannot invite someone with a role >= their own
     * - Existing pending invitations for the same email may be replaced
     * - If the email already belongs to a member, this should fail
     *
     * @param actorId ID of the user performing the action
     * @param orgId organization ID
     * @param request invite payload (email + role)
     */
    void inviteMember(UUID actorId, UUID orgId, InviteMemberRequest request);

    /**
     * Accept an invitation using a raw token.
     *
     * <p>Rules:
     * - Token must be valid, not expired, and not already used
     * - Email of accepting user must match invitation email
     * - Operation should be idempotent (accepting twice should not fail)
     *
     * @param rawToken invitation token (unhashed)
     * @param acceptingUserId ID of the user accepting the invitation
     */
    void acceptInvitation(String rawToken, UUID acceptingUserId);

    /**
     * List all members of an organization.
     *
     * <p>Rules:
     * - Requester must be a member of the organization
     *
     * @param requesterId ID of requesting user
     * @param orgId organization ID
     * @return list of members with basic profile + role
     */
    List<MemberResponse> listMembers(UUID requesterId, UUID orgId);

    /**
     * List all pending invitations for an organization.
     *
     * <p>Rules:
     * - Typically restricted to ADMIN+ roles
     * - Only active (not accepted/cancelled/expired) invitations should be returned
     *
     * @param requesterId ID of requesting user
     * @param orgId organization ID
     * @return list of pending invitations
     */
    List<InvitationResponse> listPendingInvitations(UUID requesterId, UUID orgId);

    /**
     * Change the role of an existing member.
     *
     * <p>Rules:
     * - Actor must be allowed to manage the target member
     * - Actor cannot assign a role >= their own
     * - OWNER role changes are restricted (cannot be arbitrarily reassigned)
     *
     * @param actorId ID of user performing the action
     * @param orgId organization ID
     * @param targetUserId member whose role is being changed
     * @param request new role
     * @return updated member representation
     */
    MemberResponse changeMemberRole(UUID actorId, UUID orgId, UUID targetUserId, ChangeMemberRoleRequest request);

    /**
     * Remove a member from the organization.
     *
     * <p>Rules:
     * - Actor cannot remove themselves via this method (use leaveOrganization)
     * - Actor must be allowed to manage the target member
     * - OWNER cannot be removed
     *
     * @param actorId ID of user performing the action
     * @param orgId organization ID
     * @param targetUserId member to remove
     */
    void removeMember(UUID actorId, UUID orgId, UUID targetUserId);

    /**
     * Cancel an existing invitation.
     *
     * <p>Rules:
     * - Typically restricted to ADMIN+ roles
     * - Invitation must belong to the specified organization
     *
     * @param actorId ID of user performing the action
     * @param orgId organization ID
     * @param invitationId invitation to cancel
     */
    void cancelInvitation(UUID actorId, UUID orgId, UUID invitationId);

    /**
     * Allow a user to leave an organization voluntarily.
     *
     * <p>Rules:
     * - OWNER cannot leave without transferring ownership first
     *
     * @param userId ID of user leaving
     * @param orgId organization ID
     */
    void leaveOrganization(UUID userId, UUID orgId);

    /**
     * Transfer ownership of an organization to another existing member.
     *
     * <p>Rules:
     * - Only current OWNER can perform this action
     * - Target user must already be a member
     * - After transfer:
     *   - New user becomes OWNER
     *   - Previous owner is downgraded (typically to ADMIN)
     * - Must maintain "single OWNER" invariant
     *
     * @param currentOwnerId ID of current owner
     * @param orgId organization ID
     * @param request contains new owner user ID
     */
    void transferOwnership(UUID currentOwnerId, UUID orgId, TransferOwnershipRequest request);
}