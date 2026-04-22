package com.mockify.backend.service;

import com.mockify.backend.common.enums.MemberRole;
import com.mockify.backend.dto.request.member.*;
import com.mockify.backend.dto.response.member.MemberResponse;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.exception.ForbiddenException;
import com.mockify.backend.model.*;
import com.mockify.backend.repository.*;
import com.mockify.backend.util.InvitationTokenUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrganizationMemberServiceIntegrationTest {

    @Autowired OrganizationMemberService memberService;
    @Autowired OrganizationMemberRepository memberRepo;
    @Autowired OrganizationInvitationRepository invitationRepo;
    @Autowired UserRepository userRepo;
    @Autowired OrganizationRepository orgRepo;

    private User owner;
    private User member;
    private User outsider;
    private Organization org;

    @BeforeEach
    void setUp() {
        owner    = userRepo.save(buildUser("owner@test.com"));
        member   = userRepo.save(buildUser("member@test.com"));
        outsider = userRepo.save(buildUser("outsider@test.com"));
        org      = orgRepo.save(buildOrg("Test Org", owner));

        // Seed owner as OWNER member (as the service/migration would do)
        memberRepo.save(OrganizationMember.builder()
                .organization(org).user(owner).role(MemberRole.OWNER).joinedAt(LocalDateTime.now()).build());
    }

    // ── listMembers ────────────────────────────────────────────────────────────

    @Test
    void listMembers_returns_all_members() {
        memberRepo.save(OrganizationMember.builder()
                .organization(org).user(member).role(MemberRole.DEVELOPER).joinedAt(LocalDateTime.now()).build());

        List<MemberResponse> list = memberService.listMembers(owner.getId(), org.getId());
        assertEquals(2, list.size());
    }

    @Test
    void listMembers_throws_when_not_a_member() {
        assertThrows(ForbiddenException.class,
                () -> memberService.listMembers(outsider.getId(), org.getId()));
    }

    // ── changeMemberRole ───────────────────────────────────────────────────────

    @Test
    void owner_can_promote_developer_to_admin() {
        memberRepo.save(OrganizationMember.builder()
                .organization(org).user(member).role(MemberRole.DEVELOPER).joinedAt(LocalDateTime.now()).build());

        ChangeMemberRoleRequest req = new ChangeMemberRoleRequest();
        req.setRole(MemberRole.ADMIN);
        MemberResponse updated = memberService.changeMemberRole(owner.getId(), org.getId(), member.getId(), req);

        assertEquals(MemberRole.ADMIN, updated.getRole());
    }

    @Test
    void cannot_promote_to_own_role_or_above() {
        // Seed member as ADMIN
        memberRepo.save(OrganizationMember.builder()
                .organization(org).user(member).role(MemberRole.ADMIN).joinedAt(LocalDateTime.now()).build());

        // owner is OWNER — cannot be changed
        ChangeMemberRoleRequest req = new ChangeMemberRoleRequest();
        req.setRole(MemberRole.ADMIN);
        assertThrows(ForbiddenException.class,
                () -> memberService.changeMemberRole(member.getId(), org.getId(), owner.getId(), req));
    }

    // ── removeMember ──────────────────────────────────────────────────────────

    @Test
    void owner_can_remove_developer() {
        memberRepo.save(OrganizationMember.builder()
                .organization(org).user(member).role(MemberRole.DEVELOPER).joinedAt(LocalDateTime.now()).build());

        memberService.removeMember(owner.getId(), org.getId(), member.getId());

        assertFalse(memberRepo.existsByOrganizationIdAndUserId(org.getId(), member.getId()));
    }

    @Test
    void cannot_remove_self_via_removeMember() {
        assertThrows(BadRequestException.class,
                () -> memberService.removeMember(owner.getId(), org.getId(), owner.getId()));
    }

    // ── leaveOrganization ─────────────────────────────────────────────────────

    @Test
    void member_can_leave() {
        memberRepo.save(OrganizationMember.builder()
                .organization(org).user(member).role(MemberRole.DEVELOPER).joinedAt(LocalDateTime.now()).build());

        memberService.leaveOrganization(member.getId(), org.getId());

        assertFalse(memberRepo.existsByOrganizationIdAndUserId(org.getId(), member.getId()));
    }

    @Test
    void owner_cannot_leave_without_transferring_first() {
        assertThrows(BadRequestException.class,
                () -> memberService.leaveOrganization(owner.getId(), org.getId()));
    }

    // ── transferOwnership ─────────────────────────────────────────────────────

    @Test
    void owner_can_transfer_to_existing_member() {
        memberRepo.save(OrganizationMember.builder()
                .organization(org).user(member).role(MemberRole.ADMIN).joinedAt(LocalDateTime.now()).build());

        TransferOwnershipRequest req = new TransferOwnershipRequest();
        req.setNewOwnerId(member.getId());
        memberService.transferOwnership(owner.getId(), org.getId(), req);

        assertEquals(MemberRole.OWNER,
                memberRepo.findRoleByOrganizationIdAndUserId(org.getId(), member.getId()).orElseThrow());
        assertEquals(MemberRole.ADMIN,
                memberRepo.findRoleByOrganizationIdAndUserId(org.getId(), owner.getId()).orElseThrow());
    }

    @Test
    void non_owner_cannot_transfer() {
        memberRepo.save(OrganizationMember.builder()
                .organization(org).user(member).role(MemberRole.ADMIN).joinedAt(LocalDateTime.now()).build());

        TransferOwnershipRequest req = new TransferOwnershipRequest();
        req.setNewOwnerId(owner.getId());
        assertThrows(ForbiddenException.class,
                () -> memberService.transferOwnership(member.getId(), org.getId(), req));
    }

    // ── acceptInvitation token hashing ────────────────────────────────────────

    @Test
    void accept_invitation_with_correct_token_adds_member() {
        String rawToken = UUID.randomUUID().toString();

        invitationRepo.save(OrganizationInvitation.builder()
                .organization(org)
                .email(outsider.getEmail())
                .role(MemberRole.DEVELOPER)
                .tokenHash(InvitationTokenUtil.hash(rawToken))
                .invitedBy(owner)
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build());

        memberService.acceptInvitation(rawToken, outsider.getId());

        assertTrue(memberRepo.existsByOrganizationIdAndUserId(org.getId(), outsider.getId()));
    }

    @Test
    void accept_invitation_with_wrong_token_throws() {
        String rawToken = UUID.randomUUID().toString();

        invitationRepo.save(OrganizationInvitation.builder()
                .organization(org)
                .email(outsider.getEmail())
                .role(MemberRole.DEVELOPER)
                .tokenHash(InvitationTokenUtil.hash(rawToken))
                .invitedBy(owner)
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build());

        assertThrows(Exception.class,
                () -> memberService.acceptInvitation("wrong-token", outsider.getId()));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User buildUser(String email) {
        User u = new User();
        u.setName("Test"); u.setEmail(email);
        u.setPassword("hashed"); u.setProviderName("local"); u.setEmailVerified(true);
        return u;
    }

    private Organization buildOrg(String name, User owner) {
        Organization o = new Organization();
        o.setName(name);
        o.setSlug(name.toLowerCase().replace(" ", "-") + "-" + UUID.randomUUID());
        o.setOwner(owner);
        return o;
    }
}