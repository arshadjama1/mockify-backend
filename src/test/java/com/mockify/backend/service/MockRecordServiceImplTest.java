package com.mockify.backend.service.impl;

import com.mockify.backend.dto.request.record.CreateMockRecordRequest;
import com.mockify.backend.dto.request.record.UpdateMockRecordRequest;
import com.mockify.backend.dto.response.record.MockRecordResponse;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.mapper.MockRecordMapper;
import com.mockify.backend.model.MockRecord;
import com.mockify.backend.model.MockSchema;
import com.mockify.backend.repository.MockRecordRepository;
import com.mockify.backend.repository.MockSchemaRepository;
import com.mockify.backend.service.MockAutoGenerateService;
import com.mockify.backend.service.MockValidatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MockRecordServiceImplTest {

    @Mock
    private MockRecordRepository mockRecordRepository;

    @Mock
    private MockSchemaRepository mockSchemaRepository;

    @Mock
    private MockRecordMapper mockRecordMapper;

    @Mock
    private MockValidatorService mockValidatorService;

    @Mock
    private MockAutoGenerateService autoGenerateService;

    @InjectMocks
    private MockRecordServiceImpl mockRecordService;

    private UUID userId;
    private UUID schemaId;
    private UUID recordId;

    private MockSchema schema;
    private MockRecord record;

    private CreateMockRecordRequest createRequest;
    private UpdateMockRecordRequest updateRequest;

    private MockRecordResponse response;

    private Map<String, Object> data;

    @BeforeEach
    void setUp() {

        userId = UUID.randomUUID();
        schemaId = UUID.randomUUID();
        recordId = UUID.randomUUID();

        data = new HashMap<>();
        data.put("name", "John");

        schema = new MockSchema();
        schema.setId(schemaId);
        schema.setSchemaJson(Map.of("type", "object"));

        record = new MockRecord();
        record.setId(recordId);
        record.setMockSchema(schema);

        createRequest = new CreateMockRecordRequest();
        createRequest.setData(data);

        updateRequest = new UpdateMockRecordRequest();
        updateRequest.setData(data);

        response = new MockRecordResponse();
    }

    // -------------------------------------------------------------------------
    // createRecord
    // -------------------------------------------------------------------------

    @Test
    void createRecord_ShouldCreateSuccessfully() {

        when(mockSchemaRepository.findById(schemaId))
                .thenReturn(Optional.of(schema));

        when(mockRecordMapper.toEntity(createRequest))
                .thenReturn(record);

        when(mockRecordMapper.toResponse(record))
                .thenReturn(response);

        MockRecordResponse result =
                mockRecordService.createRecord(userId, schemaId, createRequest);

        assertNotNull(result);

        verify(mockValidatorService)
                .validateRecordAgainstSchema(schema.getSchemaJson(), data);

        verify(mockRecordRepository).save(record);
    }

    @Test
    void createRecord_ShouldThrow_WhenRequestNull() {

        assertThrows(
                BadRequestException.class,
                () -> mockRecordService.createRecord(userId, schemaId, null)
        );
    }

    @Test
    void createRecord_ShouldThrow_WhenSchemaNotFound() {

        when(mockSchemaRepository.findById(schemaId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> mockRecordService.createRecord(userId, schemaId, createRequest)
        );
    }

    // -------------------------------------------------------------------------
    // createRecordsBulk
    // -------------------------------------------------------------------------

    @Test
    void createRecordsBulk_ShouldCreateSuccessfully() {

        List<CreateMockRecordRequest> requests = List.of(createRequest);

        when(mockSchemaRepository.findById(schemaId))
                .thenReturn(Optional.of(schema));

        when(mockRecordMapper.toEntity(any()))
                .thenReturn(record);

        when(mockRecordMapper.toResponse(any()))
                .thenReturn(response);

        List<MockRecordResponse> result =
                mockRecordService.createRecordsBulk(userId, schemaId, requests);

        assertEquals(1, result.size());

        verify(mockRecordRepository, times(1)).save(any());
    }

    @Test
    void createRecordsBulk_ShouldThrow_WhenEmptyList() {

        assertThrows(
                BadRequestException.class,
                () -> mockRecordService.createRecordsBulk(
                        userId,
                        schemaId,
                        Collections.emptyList()
                )
        );
    }

    // -------------------------------------------------------------------------
    // autoGenerateRecordsBulk
    // -------------------------------------------------------------------------

    @Test
    void autoGenerateRecordsBulk_ShouldGenerateSuccessfully() {

        when(mockSchemaRepository.findById(schemaId))
                .thenReturn(Optional.of(schema));

        when(autoGenerateService.generateRecord(any()))
                .thenReturn(data);

        when(mockRecordMapper.toEntity(any()))
                .thenReturn(record);

        when(mockRecordMapper.toResponse(any()))
                .thenReturn(response);

        List<MockRecordResponse> result =
                mockRecordService.autoGenerateRecordsBulk(userId, schemaId, 2);

        assertEquals(2, result.size());

        verify(autoGenerateService, times(2))
                .generateRecord(any());

        verify(mockRecordRepository, times(2))
                .save(any());
    }

    // -------------------------------------------------------------------------
    // getRecordById
    // -------------------------------------------------------------------------

    @Test
    void getRecordById_ShouldReturnRecord() {

        when(mockRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(mockRecordMapper.toResponse(record))
                .thenReturn(response);

        MockRecordResponse result =
                mockRecordService.getRecordById(userId, recordId);

        assertNotNull(result);
    }

    @Test
    void getRecordById_ShouldThrow_WhenNotFound() {

        when(mockRecordRepository.findById(recordId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> mockRecordService.getRecordById(userId, recordId)
        );
    }

    // -------------------------------------------------------------------------
    // getRecordsBySchemaId
    // -------------------------------------------------------------------------

    @Test
    void getRecordsBySchemaId_ShouldReturnPage() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<MockRecord> page =
                new PageImpl<>(List.of(record));

        when(mockRecordRepository.findByMockSchema_Id(schemaId, pageable))
                .thenReturn(page);

        when(mockRecordMapper.toResponse(any()))
                .thenReturn(response);

        Page<MockRecordResponse> result =
                mockRecordService.getRecordsBySchemaId(userId, schemaId, pageable);

        assertEquals(1, result.getTotalElements());
    }

    // -------------------------------------------------------------------------
    // updateRecord
    // -------------------------------------------------------------------------

    @Test
    void updateRecord_ShouldUpdateSuccessfully() {

        when(mockRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        when(mockRecordMapper.toResponse(record))
                .thenReturn(response);

        MockRecordResponse result =
                mockRecordService.updateRecord(userId, recordId, updateRequest);

        assertNotNull(result);

        verify(mockValidatorService)
                .validateRecordAgainstSchema(
                        schema.getSchemaJson(),
                        data
                );

        verify(mockRecordMapper)
                .updateEntityFromRequest(updateRequest, record);

        verify(mockRecordRepository)
                .save(record);
    }

    @Test
    void updateRecord_ShouldThrow_WhenRequestNull() {

        assertThrows(
                BadRequestException.class,
                () -> mockRecordService.updateRecord(userId, recordId, null)
        );
    }

    // -------------------------------------------------------------------------
    // deleteRecord
    // -------------------------------------------------------------------------

    @Test
    void deleteRecord_ShouldDeleteSuccessfully() {

        when(mockRecordRepository.findById(recordId))
                .thenReturn(Optional.of(record));

        mockRecordService.deleteRecord(userId, recordId);

        verify(mockRecordRepository).delete(record);
    }

    @Test
    void deleteRecord_ShouldThrow_WhenNotFound() {

        when(mockRecordRepository.findById(recordId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> mockRecordService.deleteRecord(userId, recordId)
        );
    }

    // -------------------------------------------------------------------------
    // countRecords
    // -------------------------------------------------------------------------

    @Test
    void countRecords_ShouldReturnCount() {

        when(mockRecordRepository.count())
                .thenReturn(5L);

        long result = mockRecordService.countRecords();

        assertEquals(5L, result);
    }

    // -------------------------------------------------------------------------
    // deleteExpiredRecords
    // -------------------------------------------------------------------------

    @Test
    void deleteExpiredRecords_ShouldDeleteExpiredRecords() throws Exception {

        List<MockRecord> expired = List.of(record);

        when(mockRecordRepository.findByExpiresAtBefore(any(LocalDateTime.class)))
                .thenReturn(expired);

        Method method = MockRecordServiceImpl.class
                .getDeclaredMethod("deleteExpiredRecords");

        method.setAccessible(true);
        method.invoke(mockRecordService);

        verify(mockRecordRepository).deleteAll(expired);
    }
}