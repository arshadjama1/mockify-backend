package com.mockify.backend.mapper.admin;

import com.mockify.backend.dto.response.admin.AdminUserResponse;
import com.mockify.backend.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminUserMapper {

    @Mapping(target = "role", source = "role")
    AdminUserResponse toResponse(User user);

    List<AdminUserResponse> toResponseList(List<User> users);
}
