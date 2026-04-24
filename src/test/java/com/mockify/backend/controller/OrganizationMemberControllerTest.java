package com.mockify.backend.controller;

import com.mockify.backend.common.enums.MemberRole;
import com.mockify.backend.common.enums.UserRole;
import com.mockify.backend.model.*;
import com.mockify.backend.repository.*;
import com.mockify.backend.security.JwtTokenProvider;
import com.mockify.backend.util.InvitationTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.mockito.Mockito;
import com.mockify.backend.service.EndpointService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrganizationMemberControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired UserRepository userRepo;
    @Autowired OrganizationRepository orgRepo;
    @Autowired OrganizationMemberRepository memberRepo;
    @Autowired OrganizationInvitationRepository invitationRepo;
    @MockitoBean EndpointService endpointService;

    private User owner;
    private User dev;
    private Organization org;
    private String ownerJwt;
    private String devJwt;

    @BeforeEach
    void setUp() {
        owner = userRepo.save(buildUser("ctrl-owner@test.com"));
        dev   = userRepo.save(buildUser("ctrl-dev@test.com"));
        org   = orgRepo.save(buildOrg("Ctrl Org", owner));

        memberRepo.save(OrganizationMember.builder()
                .organization(org).user(owner).role(MemberRole.OWNER).joinedAt(LocalDateTime.now()).build());
        memberRepo.save(OrganizationMember.builder()
                .organization(org).user(dev).role(MemberRole.DEVELOPER).joinedAt(LocalDateTime.now()).build());

        ownerJwt = jwtTokenProvider.generateAccessToken(owner.getId(), UserRole.USER);
        devJwt   = jwtTokenProvider.generateAccessToken(dev.getId(), UserRole.USER);

        // ADD THIS — stub slug → UUID resolution for all tests in this class
        Mockito.when(endpointService.resolveOrganization(org.getSlug()))
                .thenReturn(org.getId());
    }


    @Test
    void listMembers_returns_200_for_member() throws Exception {
        mockMvc.perform(get("/api/organizations/{org}/members", org.getSlug())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void listMembers_returns_403_for_non_member() throws Exception {
        User stranger = userRepo.save(buildUser("stranger@test.com"));
        String jwt = jwtTokenProvider.generateAccessToken(stranger.getId(), UserRole.USER);

        mockMvc.perform(get("/api/organizations/{org}/members", org.getSlug())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void listPendingInvitations_requires_admin_role() throws Exception {
        mockMvc.perform(get("/api/organizations/{org}/members/invitations", org.getSlug())
                        .header("Authorization", "Bearer " + devJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void listPendingInvitations_returns_200_for_owner() throws Exception {
        mockMvc.perform(get("/api/organizations/{org}/members/invitations", org.getSlug())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk());
    }

    @Test
    void invite_requires_jwt_not_api_key() throws Exception {
        // API key auth is blocked by requireJwtAuthentication()
        mockMvc.perform(post("/api/organizations/{org}/members/invite", org.getSlug())
                        .header("X-API-Key", "mk_live_fakekeyxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@x.com\",\"role\":\"DEVELOPER\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void accept_invitation_adds_user_to_org() throws Exception {
        User invitee = userRepo.save(buildUser("invitee@test.com"));
        String raw = UUID.randomUUID().toString();
        invitationRepo.save(OrganizationInvitation.builder()
                .organization(org).email(invitee.getEmail()).role(MemberRole.DEVELOPER)
                .tokenHash(InvitationTokenUtil.hash(raw)).invitedBy(owner)
                .expiresAt(LocalDateTime.now().plusHours(48)).build());

        String inviteeJwt = jwtTokenProvider.generateAccessToken(invitee.getId(), UserRole.USER);

        mockMvc.perform(post("/api/invitations/accept")
                        .param("token", raw)
                        .header("Authorization", "Bearer " + inviteeJwt))
                .andExpect(status().isOk());
    }

    @Test
    void remove_member_returns_204() throws Exception {
        mockMvc.perform(delete("/api/organizations/{org}/members/{userId}", org.getSlug(), dev.getId())
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNoContent());
    }

    private User buildUser(String email) {
        User u = new User(); u.setName("Test"); u.setEmail(email);
        u.setPassword("hashed"); u.setProviderName("local"); u.setEmailVerified(true);
        return u;
    }
    private Organization buildOrg(String name, User owner) {
        Organization o = new Organization(); o.setName(name);
        o.setSlug(name.toLowerCase().replace(" ","-")+"-"+UUID.randomUUID()); o.setOwner(owner);
        return o;
    }
}