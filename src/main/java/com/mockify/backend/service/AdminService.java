package com.mockify.backend.service;

import com.mockify.backend.dto.response.admin.*;

import java.util.List;

public interface AdminService {

    List<AdminUserResponse> listUsers();

    List<AdminOrganizationResponse> listOrganizations();

    List<AdminProjectResponse> listProjects();

    List<AdminMockSchemaResponse> listSchemas();

    List<AdminMockRecordResponse> listRecords();

}
