package com.mockify.backend.mapper;

import com.mockify.backend.dto.response.project.ProjectDetailResponse;
import com.mockify.backend.model.MockRecord;
import com.mockify.backend.model.MockSchema;
import com.mockify.backend.model.Organization;
import com.mockify.backend.model.Project;
import com.mockify.backend.dto.request.project.CreateProjectRequest;
import com.mockify.backend.dto.request.project.UpdateProjectRequest;
import com.mockify.backend.dto.response.project.ProjectResponse;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    // Entity -> Response
    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "schemaCount", expression = "java(project.getMockSchemas().size())")
    @Mapping(target = "totalRecords", expression = "java(calculateTotalRecords(project))")
    ProjectResponse toResponse(Project project);

    List<ProjectResponse> toResponseList(List<Project> projects);

    @Mapping(target = "organization", source = "organization")
    @Mapping(target = "schemas", source = "mockSchemas")
    @Mapping(target = "stats", expression = "java(calculateProjectStats(project))")
    ProjectDetailResponse toDetailResponse(Project project);

    // ===== Nested Mappings =====

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    ProjectDetailResponse.OrganizationSummary toOrganizationSummary(Organization organization);

    @Mapping(target = "recordCount", expression = "java(schema.getMockRecords().size())")
    ProjectDetailResponse.MockSchemaSummary toMockSchemaSummary(MockSchema schema);

    List<ProjectDetailResponse.MockSchemaSummary> toMockSchemaSummaryList(List<MockSchema> schemas);

    // Create Request -> Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "mockSchemas", ignore = true)
    Project toEntity(CreateProjectRequest request);

    // Update Request -> Entity
    // Updates existing entity with only non-null fields
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "mockSchemas", ignore = true)
    void updateEntityFromRequest(UpdateProjectRequest request, @MappingTarget Project entity);

    // ===== Helper Methods =====
    default int calculateTotalRecords(Project project) {
        if (project.getMockSchemas() == null) return 0;
        return project.getMockSchemas().stream()
                .mapToInt(schema -> schema.getMockRecords() != null ? schema.getMockRecords().size() : 0)
                .sum();
    }

    default ProjectDetailResponse.ProjectStats calculateProjectStats(Project project) {
        ProjectDetailResponse.ProjectStats stats = new ProjectDetailResponse.ProjectStats();
        LocalDateTime now = LocalDateTime.now();

        if (project.getMockSchemas() == null) {
            stats.setTotalSchemas(0);
            stats.setTotalRecords(0);
            stats.setActiveRecords(0);
            stats.setExpiredRecords(0);
            return stats;
        }

        stats.setTotalSchemas(project.getMockSchemas().size());

        int totalRecords = 0;
        int activeRecords = 0;

        for (MockSchema schema : project.getMockSchemas()) {
            if (schema.getMockRecords() != null) {
                for (MockRecord record : schema.getMockRecords()) {
                    totalRecords++;
                    if (record.getExpiresAt().isAfter(now)) {
                        activeRecords++;
                    }
                }
            }
        }

        stats.setTotalRecords(totalRecords);
        stats.setActiveRecords(activeRecords);
        stats.setExpiredRecords(totalRecords - activeRecords);

        return stats;
    }
}
