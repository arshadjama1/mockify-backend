package com.mockify.backend.service.impl;

import com.mockify.backend.dto.response.admin.AdminUserResponse;
import com.mockify.backend.dto.response.admin.AdminOrganizationResponse;
import com.mockify.backend.dto.response.admin.AdminProjectResponse;
import com.mockify.backend.dto.response.admin.AdminMockSchemaResponse;
import com.mockify.backend.dto.response.admin.AdminMockRecordResponse;
import com.mockify.backend.mapper.admin.*;
import com.mockify.backend.repository.*;
import com.mockify.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final MockSchemaRepository mockSchemaRepository;
    private final MockRecordRepository mockRecordRepository;

    private final AdminUserMapper userMapper;
    private final AdminOrganizationMapper organizationMapper;
    private final AdminProjectMapper projectMapper;
    private final AdminMockSchemaMapper schemaMapper;
    private final AdminMockRecordMapper recordMapper;

    @Override
    public List<AdminUserResponse> listUsers() {
        return userMapper.toResponseList(userRepository.findAll());
    }

    @Override
    public List<AdminOrganizationResponse> listOrganizations() {
        return organizationMapper.toResponseList(organizationRepository.findAll());
    }

    @Override
    public List<AdminProjectResponse> listProjects() {
        return projectMapper.toResponseList(projectRepository.findAll());
    }

    @Override
    public List<AdminMockSchemaResponse> listSchemas() {
        return schemaMapper.toResponseList(mockSchemaRepository.findAll());
    }

    @Override
    public List<AdminMockRecordResponse> listRecords() {
        return recordMapper.toResponseList(mockRecordRepository.findAll());
    }

    // TODO: Leter we add promote/demote users
}
