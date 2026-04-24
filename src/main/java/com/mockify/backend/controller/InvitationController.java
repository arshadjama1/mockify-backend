package com.mockify.backend.controller;

import com.mockify.backend.security.SecurityUtils;
import com.mockify.backend.service.OrganizationMemberService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/invitations")
@Tag(name = "Organization Members",  description = "Accept organization invitations via token-based onboarding.")
public class InvitationController {

    private final OrganizationMemberService memberService;

    @PostMapping("/accept")
    public ResponseEntity<Void> acceptInvitation(
            @RequestParam String token,
            Authentication auth) {
        UUID userId = SecurityUtils.resolveUserId(auth);
        memberService.acceptInvitation(token, userId);
        return ResponseEntity.ok().build();
    }
}