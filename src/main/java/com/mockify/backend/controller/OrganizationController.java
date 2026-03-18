package com.mockify.backend.controller;

import com.mockify.backend.dto.request.organization.CreateOrganizationRequest;
import com.mockify.backend.dto.request.organization.UpdateOrganizationRequest;
import com.mockify.backend.dto.response.organization.OrganizationDetailResponse;
import com.mockify.backend.dto.response.organization.OrganizationResponse;
import com.mockify.backend.dto.response.page.PageResponse;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.OrganizationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    // Create organization for logged-in user
    @PostMapping("/organizations")
    public ResponseEntity<OrganizationResponse> createOrganization(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateOrganizationRequest request) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("User ID {} creating organization: {}", userId, request.getName());

        OrganizationResponse response = organizationService.createOrganization(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Get details of a specific organization
    @GetMapping("/organizations/{org}")
    public ResponseEntity<OrganizationDetailResponse> getOrganization(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String org) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID orgId = endpointService.resolveOrganization(org);

        log.debug("User {} fetching organization {}", userId, orgId);

        OrganizationDetailResponse response =
                organizationService.getOrganizationDetail(orgId, userId);

        return ResponseEntity.ok(response);
    }

    // Get all organizations for current user
    @GetMapping("/organizations")
    public ResponseEntity<PageResponse<OrganizationResponse>> getMyOrganizations(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());

        Page<OrganizationResponse> page = organizationService.getMyOrganizations(userId, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    // Update organization
    @PutMapping("/organizations/{org}")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String org,
            @Valid @RequestBody UpdateOrganizationRequest request) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID orgId = endpointService.resolveOrganization(org);

        OrganizationResponse updated =
                organizationService.updateOrganization(userId, orgId, request);

        return ResponseEntity.ok(updated);
    }

    // Delete organization
    @DeleteMapping("/organizations/{org}")
    public ResponseEntity<Void> deleteOrganization(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String org) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        UUID orgId = endpointService.resolveOrganization(org);

        organizationService.deleteOrganization(userId, orgId);
        return ResponseEntity.noContent().build();
    }
}
