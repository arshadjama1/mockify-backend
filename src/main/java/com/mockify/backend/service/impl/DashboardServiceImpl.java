package com.mockify.backend.service.impl;

import com.mockify.backend.dto.response.dashboard.*;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.model.MockSchema;
import com.mockify.backend.model.Organization;
import com.mockify.backend.model.Project;
import com.mockify.backend.repository.*;
import com.mockify.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private LocalDateTime now;
    private final DashboardRepository dashboardRepository;
    private final AccessControlServiceImpl accessControlService;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final MockSchemaRepository mockSchemaRepository;
    private final MockRecordRepository mockRecordRepository;

    @Override
    public UserStats userStats(UUID userId) {
        return dashboardRepository.userStats(userId);
    }

    @Override
    public OrganizationStats organizationStats(UUID userId, UUID orgId) {

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        accessControlService.checkOrganizationAccess(userId, org, "Organization");

        return dashboardRepository.organizationStats(orgId);
    }

    @Override
    public ProjectStats projectStats(UUID userId, UUID projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        accessControlService.checkOrganizationAccess(
                userId,
                project.getOrganization(),
                "Project"
        );

        now = LocalDateTime.now();
        return dashboardRepository.projectStats(projectId, now);
    }

    @Override
    public SchemaStats schemaStats(UUID userId, UUID schemaId) {

        MockSchema schema = mockSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));

        accessControlService.checkOrganizationAccess(
                userId,
                schema.getProject().getOrganization(),
                "Schema"
        );

        now = LocalDateTime.now();
        return dashboardRepository.schemaStats(schemaId, now, now.plusMinutes(60));
    }

    @Override
    public RecordHealthStats recordHealth(UUID userId) {
        now = LocalDateTime.now();
        return dashboardRepository.recordHealthStats(userId, now, now.plusMinutes(60));
    }
}