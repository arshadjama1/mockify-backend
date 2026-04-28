package com.mockify.backend.controller;

import com.mockify.backend.common.enums.UserRole;
import com.mockify.backend.dto.response.admin.*;
import com.mockify.backend.dto.response.page.PageResponse;
import com.mockify.backend.service.AdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Tag(name = "Admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /*
         Basic admin health endpoint.
         Used to verify ADMIN access works.
     */
    @GetMapping("/ping")
    public Map<String, Object> adminPing(){
        return Map.of(
                "status", "ok",
                "message", "Admin access granted"
        );
    }

    @GetMapping("/users")
    public ResponseEntity<PageResponse<AdminUserResponse>> getUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) UserRole role,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        Page<AdminUserResponse> page =  adminService.listUsers(email, role, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/organizations")
    public ResponseEntity<PageResponse<AdminOrganizationResponse>> getOrganizations(
            @RequestParam(required = false) UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        Page<AdminOrganizationResponse> page = adminService.listOrganizations(userId, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/projects")
    public ResponseEntity<PageResponse<AdminProjectResponse>> getProjects(
            @RequestParam(required = false) UUID orgId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        Page<AdminProjectResponse> page = adminService.listProjects(orgId, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/schemas")
    public ResponseEntity<PageResponse<AdminMockSchemaResponse>> getSchemas(
            @RequestParam(required = false) UUID projectId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        Page<AdminMockSchemaResponse> page = adminService.listSchemas(projectId, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/records")
    public ResponseEntity<PageResponse<AdminMockRecordResponse>> getRecords(
            @RequestParam(required = false) UUID schemaId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        Page<AdminMockRecordResponse> page = adminService.listRecords(schemaId, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    // TODO: Add promote/demote users
}
