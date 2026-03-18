package com.mockify.backend.controller;

import com.mockify.backend.dto.response.dashboard.*;
import com.mockify.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/user")
    public ResponseEntity<UserStats> userStats(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(
                dashboardService.userStats(userId)
        );
    }

    @GetMapping("/organization/{orgId}")
    public ResponseEntity<OrganizationStats> organizationStats(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(
                dashboardService.organizationStats(userId, orgId)
        );
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ProjectStats> projectStats(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());

        return ResponseEntity.ok(
                dashboardService.projectStats(userId, projectId)
        );
    }

    @GetMapping("/schema/{schemaId}")
    public ResponseEntity<SchemaStats> schemaStats(
            @PathVariable UUID schemaId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());

        return ResponseEntity.ok(
                dashboardService.schemaStats(userId, schemaId)
        );
    }

    @GetMapping("/records/health")
    public ResponseEntity<RecordHealthStats> recordHealth(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(
                dashboardService.recordHealth(userId)
        );
    }
}
