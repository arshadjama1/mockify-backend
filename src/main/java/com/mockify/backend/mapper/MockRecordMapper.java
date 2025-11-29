package com.mockify.backend.mapper;

import com.mockify.backend.model.MockRecord;
import com.mockify.backend.dto.request.record.CreateMockRecordRequest;
import com.mockify.backend.dto.request.record.UpdateMockRecordRequest;
import com.mockify.backend.dto.response.record.MockRecordResponse;
import org.mapstruct.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface MockRecordMapper {

    // Entity -> Response
    @Mapping(target = "schemaId", source = "mockSchema.id")
    @Mapping(target = "schemaName", source = "mockSchema.name")
    @Mapping(target = "expired", expression = "java(isExpired(record))")
    @Mapping(target = "ttlMinutes", expression = "java(calculateTtl(record))")
    MockRecordResponse toResponse(MockRecord record);
    List<MockRecordResponse> toResponseList(List<MockRecord> records);

    // Create Request -> Entity
    @Mapping(target = "id", ignore = true) //
    @Mapping(target = "mockSchema", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    MockRecord toEntity(CreateMockRecordRequest request);

    // Update Request -> Entity
    // Updates existing entity with only non-null fields
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "mockSchema", ignore = true)
    void updateEntityFromRequest(UpdateMockRecordRequest request, @MappingTarget MockRecord entity);

    // ===== Helper Methods =====
    default boolean isExpired(MockRecord record) {
        return record.getExpiresAt().isBefore(LocalDateTime.now());
    }

    default int calculateTtl(MockRecord record) {
        return (int) Duration.between(record.getCreatedAt(), record.getExpiresAt()).toMinutes();
    }
}
