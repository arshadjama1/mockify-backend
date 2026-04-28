package com.mockify.backend.service;

import com.mockify.backend.common.enums.UserRole;
import com.mockify.backend.dto.response.admin.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AdminService {

    Page<AdminUserResponse> listUsers(String email, UserRole role, Pageable pageable);

    Page<AdminOrganizationResponse> listOrganizations(UUID userId, Pageable pageable);

    Page<AdminProjectResponse> listProjects(UUID orgId, Pageable pageable);

    Page<AdminMockSchemaResponse> listSchemas(UUID projectId, Pageable pageable);

    Page<AdminMockRecordResponse> listRecords(UUID schemaId, Pageable pageable);

}
