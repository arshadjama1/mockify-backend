package com.mockify.backend.mapper;

import com.mockify.backend.dto.request.schema.CreateSchemaTemplateRequest;
import com.mockify.backend.dto.request.schema.CreateSchemaTemplateRequest;
import com.mockify.backend.dto.response.schema.SchemaTemplateResponse;
import com.mockify.backend.dto.response.schema.SchemaTemplateResponse;
import com.mockify.backend.model.SchemaTemplate;
import org.mapstruct.*;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface SchemaTemplateMapper {

    // Entity → Response

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "organizationName", source = "organization.name")
    SchemaTemplateResponse toResponse(SchemaTemplate template);

    java.util.List<SchemaTemplateResponse> toResponseList(java.util.List<SchemaTemplate> templates);


    // Request → Entity

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "systemTemplate", constant = "false")
    @Mapping(target = "createdAt", expression = "java(now())")
    @Mapping(target = "updatedAt", expression = "java(now())")
    SchemaTemplate toEntity(CreateSchemaTemplateRequest request);


    // Update support (future)

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "systemTemplate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(now())")
    void updateEntityFromRequest(CreateSchemaTemplateRequest request,
                                 @MappingTarget SchemaTemplate entity);


    // Helpers

    default LocalDateTime now() {
        return LocalDateTime.now();
    }
}
