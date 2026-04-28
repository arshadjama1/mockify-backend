package com.mockify.backend.service.impl;

import com.mockify.backend.common.validation.PageableValidator;
import com.mockify.backend.dto.response.record.MockRecordResponse;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.mapper.MockRecordMapper;
import com.mockify.backend.model.MockRecord;
import com.mockify.backend.model.MockSchema;
import com.mockify.backend.model.Organization;
import com.mockify.backend.model.Project;
import com.mockify.backend.repository.MockRecordRepository;
import com.mockify.backend.repository.MockSchemaRepository;
import com.mockify.backend.repository.OrganizationRepository;
import com.mockify.backend.repository.ProjectRepository;
import com.mockify.backend.service.PublicMockRecordService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicMockRecordServiceImpl implements PublicMockRecordService {

    private final MockRecordRepository mockRecordRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final MockSchemaRepository mockSchemaRepository;
    private final MockRecordMapper mockRecordMapper;

    @Override
    @Transactional(readOnly = true)
    public MockRecordResponse getRecordById(UUID schemaId, UUID recordId) {
        log.info("Public user requesting recordId={} for schemaId={}", recordId, schemaId);

        MockRecord record = mockRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));

        // Ensure schemaId matches
        if (!record.getMockSchema().getId().equals(schemaId)) {
            throw new ResourceNotFoundException("Record does not belong to the given schema");
        }

        return mockRecordMapper.toResponse(record);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MockRecordResponse> getRecordsBySchemaId(UUID schemaId, Pageable pageable) {

        // Validate Page size, protect from abuse
        PageableValidator.validate(pageable, 20);

        Page<MockRecord> recordsPage = mockRecordRepository.findByMockSchema_Id(schemaId, pageable);

        log.info("Public user fetching records page={}, size={} under schemaId {}",
                recordsPage.getNumber(),
                recordsPage.getSize(),
                schemaId);

        return recordsPage.map(mockRecordMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MockRecordResponse> getRecordsBySlug(
            String orgSlug,
            String projectSlug,
            String schemaSlug,
            Pageable pageable
    ) {
        log.info(
                "Public user requesting records for org={}, project={}, schema={}",
                orgSlug, projectSlug, schemaSlug
        );

        // Validate Page size, protect from abuse
        PageableValidator.validate(pageable, 20);

        Organization organization = organizationRepository.findBySlug(orgSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Project project = projectRepository.findBySlugAndOrganizationId(
                projectSlug,
                organization.getId()
        ).orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        MockSchema schema = mockSchemaRepository.findBySlugAndProjectId(
                schemaSlug,
                project.getId()
        ).orElseThrow(() -> new ResourceNotFoundException("Schema not found"));

        Page<MockRecord> recordsPage =
                mockRecordRepository.findByMockSchema_Id(schema.getId(), pageable);

        log.info("Public user fetching records page={}, size={} under schema {}",
                recordsPage.getNumber(),
                recordsPage.getSize(),
                schemaSlug);

        return recordsPage.map(mockRecordMapper::toResponse);
    }
}
