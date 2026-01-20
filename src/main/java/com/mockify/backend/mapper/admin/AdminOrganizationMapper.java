package com.mockify.backend.mapper.admin;

import com.mockify.backend.dto.response.admin.AdminOrganizationResponse;
import com.mockify.backend.model.Organization;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminOrganizationMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerName", source = "owner.name")
    @Mapping(target = "projectCount", expression = "java(organization.getProjects().size())")
    AdminOrganizationResponse toResponse(Organization organization);

    List<AdminOrganizationResponse> toResponseList(List<Organization> organizations);
}