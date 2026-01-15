package com.mockify.backend.mapper.admin;

import com.mockify.backend.dto.response.admin.AdminProjectResponse;
import com.mockify.backend.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminProjectMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "schemaCount", expression = "java(project.getMockSchemas().size())")
    AdminProjectResponse toResponse(Project project);

    List<AdminProjectResponse> toResponseList(List<Project> projects);
}
