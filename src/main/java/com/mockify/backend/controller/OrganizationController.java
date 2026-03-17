package com.mockify.backend.controller;

import com.mockify.backend.dto.request.organization.CreateOrganizationRequest;
import com.mockify.backend.dto.request.organization.UpdateOrganizationRequest;
import com.mockify.backend.dto.response.organization.OrganizationDetailResponse;
import com.mockify.backend.dto.response.organization.OrganizationResponse;
import com.mockify.backend.security.ApiKeyAuthenticationToken;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.OrganizationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Organization")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final EndpointService endpointService;

    private UUID resolveUserId(Authentication auth) {
        if (auth instanceof ApiKeyAuthenticationToken token) {
            return token.getOwnerId();
        } else if (auth.getPrincipal() instanceof UserDetails user) {
            return UUID.fromString(user.getUsername());
        }
        throw new AccessDeniedException("Unknown authentication type");
    }

    // Create organization for logged-in user
    @PostMapping("/organizations")
    public ResponseEntity<OrganizationResponse> createOrganization(
            Authentication auth,
            @Valid @RequestBody CreateOrganizationRequest request) {

        UUID userId = resolveUserId(auth);
        log.info("User ID {} creating organization: {}", userId, request.getName());

        OrganizationResponse response = organizationService.createOrganization(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Get details of a specific organization
    @GetMapping("/organizations/{org}")
    public ResponseEntity<OrganizationDetailResponse> getOrganization(
            Authentication auth,
            @PathVariable String org) {

        UUID userId = resolveUserId(auth);
        UUID orgId = endpointService.resolveOrganization(org);

        log.debug("User {} fetching organization {}", userId, orgId);

        OrganizationDetailResponse response = organizationService.getOrganizationDetail(orgId, userId);
        return ResponseEntity.ok(response);
    }

    // Get all organizations for current user
    @GetMapping("/organizations")
    public ResponseEntity<List<OrganizationResponse>> getMyOrganizations(Authentication auth) {

        UUID userId = resolveUserId(auth);
        List<OrganizationResponse> responses = organizationService.getMyOrganizations(userId);
        return ResponseEntity.ok(responses);
    }

    // Update organization
    @PutMapping("/organizations/{org}")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            Authentication auth,
            @PathVariable String org,
            @Valid @RequestBody UpdateOrganizationRequest request) {

        UUID userId = resolveUserId(auth);
        UUID orgId = endpointService.resolveOrganization(org);

        OrganizationResponse updated = organizationService.updateOrganization(userId, orgId, request);
        return ResponseEntity.ok(updated);
    }

    // Delete organization
    @DeleteMapping("/organizations/{org}")
    public ResponseEntity<Void> deleteOrganization(
            Authentication auth,
            @PathVariable String org) {

        UUID userId = resolveUserId(auth);
        UUID orgId = endpointService.resolveOrganization(org);

        organizationService.deleteOrganization(userId, orgId);
        return ResponseEntity.noContent().build();
    }
}