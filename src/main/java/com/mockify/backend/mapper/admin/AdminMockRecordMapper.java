package com.mockify.backend.mapper.admin;

import com.mockify.backend.dto.response.admin.AdminMockRecordResponse;
import com.mockify.backend.model.MockRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminMockRecordMapper {

    @Mapping(target = "schemaId", source = "mockSchema.id")
    AdminMockRecordResponse toResponse(MockRecord record);

    List<AdminMockRecordResponse> toResponseList(List<MockRecord> records);
}

