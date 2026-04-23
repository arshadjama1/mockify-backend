package com.mockify.backend.security;

import com.mockify.backend.common.enums.MemberRole;
import com.mockify.backend.model.*;
import com.mockify.backend.repository.*;
import com.mockify.backend.service.MockSchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MembershipPermissionEvaluatorTest {

    @Autowired MockSchemaService mockSchemaService;
    @Autowired MockSchemaRepository mockSchemaRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired UserRepository userRepository;
    @Autowired OrganizationMemberRepository memberRepository;
    @MockitoBean com.mockify.backend.service.EndpointService endpointService;

    private User owner;
    private Organization organization;
    private Project project;
    private MockSchema schema;

    @BeforeEach
    void setUp() {
        owner   = userRepository.save(buildUser("perm-owner@test.com"));
        organization     = organizationRepository.save(buildOrg("Perm Test Org", owner));
        project = projectRepository.save(buildProject("Perm Project", organization));
        schema  = mockSchemaRepository.save(buildSchema("Perm Schema", project));

        // Enroll owner
        memberRepository.save(OrganizationMember.builder()
                .organization(organization).user(owner).role(MemberRole.OWNER).joinedAt(LocalDateTime.now()).build());
    }

    @Test
    void owner_can_read_schema() {
        authenticateAsJwt(owner.getId());
        assertThatNoException().isThrownBy(
                () -> mockSchemaService.getSchemaById(owner.getId(), schema.getId()));
    }

    @Test
    void developer_member_can_read_schema() {
        User dev = userRepository.save(buildUser("dev@test.com"));
        memberRepository.save(OrganizationMember.builder()
                .organization(organization).user(dev).role(MemberRole.DEVELOPER).joinedAt(LocalDateTime.now()).build());

        authenticateAsJwt(dev.getId());
        assertThatNoException().isThrownBy(
                () -> mockSchemaService.getSchemaById(dev.getId(), schema.getId()));
    }

    @Test
    void viewer_member_cannot_delete_schema() {
        User viewer = userRepository.save(buildUser("viewer@test.com"));
        memberRepository.save(OrganizationMember.builder()
                .organization(organization).user(viewer).role(MemberRole.VIEWER).joinedAt(LocalDateTime.now()).build());

        authenticateAsJwt(viewer.getId());
        assertThatThrownBy(
                () -> mockSchemaService.deleteSchema(viewer.getId(), schema.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void non_member_jwt_cannot_read_schema() {
        User stranger = userRepository.save(buildUser("stranger@test.com"));
        authenticateAsJwt(stranger.getId());
        assertThatThrownBy(
                () -> mockSchemaService.getSchemaById(stranger.getId(), schema.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void developer_cannot_delete_schema() {
        User dev = userRepository.save(buildUser("dev2@test.com"));
        memberRepository.save(OrganizationMember.builder()
                .organization(organization).user(dev).role(MemberRole.DEVELOPER).joinedAt(LocalDateTime.now()).build());

        authenticateAsJwt(dev.getId());
        assertThatThrownBy(
                () -> mockSchemaService.deleteSchema(dev.getId(), schema.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    private void authenticateAsJwt(UUID userId) {
        var principal = org.springframework.security.core.userdetails.User
                .withUsername(userId.toString()).password("")
                .authorities(new SimpleGrantedAuthority("ROLE_USER")).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
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
    private Project buildProject(String name, Organization org) {
        Project p = new Project(); p.setName(name);
        p.setSlug(name.toLowerCase().replace(" ","-")+"-"+UUID.randomUUID()); p.setOrganization(org);
        return p;
    }
    private MockSchema buildSchema(String name, Project project) {
        MockSchema s = new MockSchema(); s.setName(name);
        s.setSlug(name.toLowerCase().replace(" ","-")+"-"+UUID.randomUUID());
        s.setSchemaJson(Map.of("field", "string")); s.setProject(project);
        return s;
    }
}