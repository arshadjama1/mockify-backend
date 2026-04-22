package com.mockify.backend.service.impl;

import com.mockify.backend.common.enums.MemberRole;
import com.mockify.backend.dto.request.member.*;
import com.mockify.backend.dto.response.member.*;
import com.mockify.backend.exception.*;
import com.mockify.backend.model.*;
import com.mockify.backend.repository.*;
import com.mockify.backend.service.MailService;
import com.mockify.backend.service.OrganizationMemberService;
import com.mockify.backend.util.InvitationTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationMemberServiceImpl implements OrganizationMemberService {

    private final OrganizationMemberRepository memberRepo;
    private final OrganizationInvitationRepository invitationRepo;
    private final OrganizationRepository orgRepo;
    private final UserRepository userRepo;
    private final MailService mailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional
    public void inviteMember(UUID actorId, UUID orgId, InviteMemberRequest request) {
        // normalize email at service boundary before any lookup or storage
        String email = request.getEmail().trim().toLowerCase();

        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        MemberRole actorRole = resolveRole(actorId, orgId);

        if (!actorRole.canInviteAs(request.getRole())) {
            throw new ForbiddenException(
                    "You need ADMIN or higher role to invite members, " +
                            "and cannot invite someone to a role equal to or above your own.");
        }

        // If target email already has an account and is already a member, reject
        userRepo.findByEmail(email).ifPresent(user -> {
            if (memberRepo.existsByOrganizationIdAndUserId(orgId, user.getId())) {
                throw new DuplicateResourceException("User is already a member of this organization");
            }
        });

        // Cancel any existing pending invitation for same email + org
        invitationRepo
                .findPendingInvitationByEmail(orgId, email)
                .ifPresent(existing -> {
                    existing.setCancelledAt(LocalDateTime.now());
                    invitationRepo.save(existing);
                });

        String rawToken   = UUID.randomUUID().toString();
        String tokenHash  = InvitationTokenUtil.hash(rawToken);

        User inviter = userRepo.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        OrganizationInvitation invitation = OrganizationInvitation.builder()
                .organization(org)
                .email(email)
                .role(request.getRole())
                .tokenHash(tokenHash)
                .invitedBy(inviter)
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build();

        try {
            invitationRepo.saveAndFlush(invitation);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException(
                    "An active invitation already exists for this email address");
        }

        String acceptLink = frontendUrl + "/invitations/accept?token=" + rawToken;
        mailService.sendInvitationEmail(
                email,
                org.getName(),
                inviter.getName(),
                request.getRole().name(),
                acceptLink);

        log.info("Invitation sent to {} for org {} with role {}", email, orgId, request.getRole());
    }

    @Override
    @Transactional
    public void acceptInvitation(String rawToken, UUID acceptingUserId) {
        OrganizationInvitation invitation = invitationRepo
                .findPendingInvitationByTokenHash(InvitationTokenUtil.hash(rawToken), LocalDateTime.now())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired invitation"));

        User user = userRepo.findById(acceptingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmail().trim().toLowerCase()
                .equals(invitation.getEmail().trim().toLowerCase())) {
            throw new ForbiddenException(
                    "This invitation was sent to a different email address");
        }

        // Idempotent: already a member → mark accepted and return cleanly
        if (memberRepo.existsByOrganizationIdAndUserId(
                invitation.getOrganization().getId(), acceptingUserId)) {
            invitation.setAcceptedAt(LocalDateTime.now());
            invitationRepo.save(invitation);
            return;
        }

        OrganizationMember member = OrganizationMember.builder()
                .organization(invitation.getOrganization())
                .user(user)
                .role(invitation.getRole())
                .invitedBy(invitation.getInvitedBy())
                .joinedAt(LocalDateTime.now())
                .build();

        memberRepo.save(member);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitationRepo.save(invitation);

        log.info("User {} accepted invitation to org {}",
                acceptingUserId, invitation.getOrganization().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UUID requesterId, UUID orgId) {
        requireMembership(requesterId, orgId);

        return memberRepo.findByOrganizationId(orgId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponse> listPendingInvitations(UUID requesterId, UUID orgId) {
        MemberRole role = resolveRole(requesterId, orgId);

        if (!role.atLeast(MemberRole.ADMIN)) {
            throw new ForbiddenException("Only ADMIN or above can view pending invitations");
        }

        return invitationRepo
                .findPendingInvitationsByOrganizationId(orgId)
                .stream()
                .filter(OrganizationInvitation::isPending)
                .map(this::toInvitationResponse)
                .toList();
    }

    @Override
    @Transactional
    public MemberResponse changeMemberRole(UUID actorId, UUID orgId,
                                           UUID targetUserId, ChangeMemberRoleRequest request) {
        MemberRole actorRole = resolveRole(actorId, orgId);

        OrganizationMember target = memberRepo
                .findByOrganizationIdAndUserId(orgId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (!actorRole.canManage(target.getRole())) {
            throw new ForbiddenException("You cannot change the role of this member");
        }
        if (!actorRole.canInviteAs(request.getRole())) {
            throw new ForbiddenException("You cannot assign a role equal to or above your own");
        }

        target.setRole(request.getRole());
        memberRepo.save(target);

        log.info("Actor {} changed role of user {} in org {} to {}",
                actorId, targetUserId, orgId, request.getRole());
        return toMemberResponse(target);
    }

    @Override
    @Transactional
    public void removeMember(UUID actorId, UUID orgId, UUID targetUserId) {
        MemberRole actorRole = resolveRole(actorId, orgId);

        OrganizationMember target = memberRepo
                .findByOrganizationIdAndUserId(orgId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (actorId.equals(targetUserId)) {
            throw new BadRequestException("Use the leave endpoint to remove yourself");
        }
        if (!actorRole.canManage(target.getRole())) {
            throw new ForbiddenException("You cannot remove this member");
        }

        memberRepo.delete(target);
        log.warn("User {} removed from org {} by actor {}", targetUserId, orgId, actorId);
    }

    @Override
    @Transactional
    public void cancelInvitation(UUID actorId, UUID orgId, UUID invitationId) {
        MemberRole actorRole = resolveRole(actorId, orgId);

        if (!actorRole.atLeast(MemberRole.ADMIN)) {
            throw new ForbiddenException("Only ADMIN or above can cancel invitations");
        }

        OrganizationInvitation invitation = invitationRepo.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (!invitation.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Invitation not found");
        }

        invitation.setCancelledAt(LocalDateTime.now());
        invitationRepo.save(invitation);

        log.info("Invitation {} cancelled by actor {} in org {}", invitationId, actorId, orgId);
    }

    @Override
    @Transactional
    public void leaveOrganization(UUID userId, UUID orgId) {
        OrganizationMember member = memberRepo
                .findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "You are not a member of this organization"));

        if (member.getRole() == MemberRole.OWNER) {
            throw new BadRequestException(
                    "Owners cannot leave. Transfer ownership first, then leave.");
        }

        memberRepo.delete(member);
        log.info("User {} left org {}", userId, orgId);
    }

    @Override
    @Transactional
    public void transferOwnership(UUID currentOwnerId, UUID orgId,
                                  TransferOwnershipRequest request) {
        MemberRole actorRole = resolveRole(currentOwnerId, orgId);

        if (actorRole != MemberRole.OWNER) {
            throw new ForbiddenException("Only the current OWNER can transfer ownership");
        }

        OrganizationMember newOwnerMember = memberRepo
                .findByOrganizationIdAndUserId(orgId, request.getNewOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Target user is not a member of this organization"));

        OrganizationMember currentOwner = memberRepo
                .findByOrganizationIdAndUserId(orgId, currentOwnerId)
                .orElseThrow();

        // saveAndFlush the demotion first
        currentOwner.setRole(MemberRole.ADMIN);
        memberRepo.saveAndFlush(currentOwner);

        // Safe to promote now — previous OWNER row is already committed as ADMIN
        newOwnerMember.setRole(MemberRole.OWNER);
        memberRepo.save(newOwnerMember);

        // Keep legacy owner_id column in sync for backward compatibility
        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        org.setOwner(newOwnerMember.getUser());
        orgRepo.save(org);

        log.warn("Ownership of org {} transferred from {} to {}",
                orgId, currentOwnerId, request.getNewOwnerId());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private MemberRole resolveRole(UUID userId, UUID orgId) {
        return memberRepo.findRoleByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new ForbiddenException(
                        "You are not a member of this organization"));
    }

    private void requireMembership(UUID userId, UUID orgId) {
        if (!memberRepo.existsByOrganizationIdAndUserId(orgId, userId)) {
            throw new ForbiddenException("You are not a member of this organization");
        }
    }

    private MemberResponse toMemberResponse(OrganizationMember m) {
        return MemberResponse.builder()
                .id(m.getId())
                .userId(m.getUser().getId())
                .name(m.getUser().getName())
                .email(m.getUser().getEmail())
                .avatarUrl(m.getUser().getAvatarUrl())
                .role(m.getRole())
                .joinedAt(m.getJoinedAt())
                .build();
    }

    private InvitationResponse toInvitationResponse(OrganizationInvitation i) {
        return InvitationResponse.builder()
                .id(i.getId())
                .email(i.getEmail())
                .role(i.getRole())
                .invitedByName(i.getInvitedBy().getName())
                .expiresAt(i.getExpiresAt())
                .createdAt(i.getCreatedAt())
                .build();
    }
}