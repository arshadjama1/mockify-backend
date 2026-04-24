package com.mockify.backend.controller;

import com.mockify.backend.dto.request.member.*;
import com.mockify.backend.dto.response.member.*;
import com.mockify.backend.security.SecurityUtils;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.OrganizationMemberService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/organizations/{org}/members")
@Tag(name = "Organization Members", description = "Manage team membership and invitations")
public class OrganizationMemberController {

    private final OrganizationMemberService memberService;
    private final EndpointService endpointService;

    @GetMapping
    public ResponseEntity<List<MemberResponse>> listMembers(
            @PathVariable String org, Authentication auth) {
        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID orgId  = endpointService.resolveOrganization(org);
        return ResponseEntity.ok(memberService.listMembers(userId, orgId));
    }

    @GetMapping("/invitations")
    public ResponseEntity<List<InvitationResponse>> listInvitations(
            @PathVariable String org, Authentication auth) {
        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID orgId  = endpointService.resolveOrganization(org);
        return ResponseEntity.ok(memberService.listPendingInvitations(userId, orgId));
    }

    @PostMapping("/invite")
    public ResponseEntity<Void> invite(
            @PathVariable String org,
            @Valid @RequestBody InviteMemberRequest request,
            Authentication auth) {
        SecurityUtils.requireJwtAuthentication(auth);
        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID orgId  = endpointService.resolveOrganization(org);
        memberService.inviteMember(userId, orgId, request);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/invitations/{invitationId}")
    public ResponseEntity<Void> cancelInvitation(
            @PathVariable String org,
            @PathVariable UUID invitationId,
            Authentication auth) {
        SecurityUtils.requireJwtAuthentication(auth);
        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID orgId  = endpointService.resolveOrganization(org);
        memberService.cancelInvitation(userId, orgId, invitationId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{targetUserId}/role")
    public ResponseEntity<MemberResponse> changeMemberRole(
            @PathVariable String org,
            @PathVariable UUID targetUserId,
            @Valid @RequestBody ChangeMemberRoleRequest request,
            Authentication auth) {
        SecurityUtils.requireJwtAuthentication(auth);
        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID orgId  = endpointService.resolveOrganization(org);
        return ResponseEntity.ok(memberService.changeMemberRole(userId, orgId, targetUserId, request));
    }

    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String org,
            @PathVariable UUID targetUserId,
            Authentication auth) {
        SecurityUtils.requireJwtAuthentication(auth);
        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID orgId  = endpointService.resolveOrganization(org);
        memberService.removeMember(userId, orgId, targetUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leaveOrganization(
            @PathVariable String org, Authentication auth) {
        SecurityUtils.requireJwtAuthentication(auth);
        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID orgId  = endpointService.resolveOrganization(org);
        memberService.leaveOrganization(userId, orgId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transfer-ownership")
    public ResponseEntity<Void> transferOwnership(
            @PathVariable String org,
            @Valid @RequestBody TransferOwnershipRequest request,
            Authentication auth) {
        SecurityUtils.requireJwtAuthentication(auth);
        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID orgId  = endpointService.resolveOrganization(org);
        memberService.transferOwnership(userId, orgId, request);
        return ResponseEntity.noContent().build();
    }
}