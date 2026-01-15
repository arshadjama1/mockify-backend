package com.mockify.backend.mapper.admin;

import com.mockify.backend.dto.response.admin.AdminMockSchemaResponse;
import com.mockify.backend.model.MockSchema;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminMockSchemaMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectName", source = "project.name")
    @Mapping(target = "recordCount", expression = "java(schema.getMockRecords().size())")
    AdminMockSchemaResponse toResponse(MockSchema schema);

    List<AdminMockSchemaResponse> toResponseList(List<MockSchema> schemas);
}
