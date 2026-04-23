package com.mockify.backend.controller;

import com.mockify.backend.dto.response.page.PageResponse;
import com.mockify.backend.dto.response.record.MockRecordResponse;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.PublicMockRecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/mock")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Public MockRecord")
public class PublicMockRecordController {

    private final PublicMockRecordService publicMockRecordService;
    private final EndpointService endpointService;

    /**
     * Get a record by ID (Public/Free User)
     */
    @GetMapping("/{org}/{project}/{schema}/records/{recordId}")
    public ResponseEntity<MockRecordResponse> getRecord(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @PathVariable UUID recordId) {

        log.info("Public user fetching recordId={} for schemaId={}", recordId, schema);

        UUID schemaId = endpointService.resolveSchema(org, project, schema);
        MockRecordResponse record = publicMockRecordService.getRecordById(schemaId, recordId);
        return ResponseEntity.ok(record);
    }

    /**
     * Get all records under a schema (Public/Free User)
     */
    @GetMapping("/{org}/{project}/{schema}/records")
    public ResponseEntity<PageResponse<MockRecordResponse>> getRecords(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @PageableDefault(size = 5, sort = "createdAt") Pageable pageable
            ) {


        UUID schemaId = endpointService.resolveSchema(org, project, schema);
        Page<MockRecordResponse> page = publicMockRecordService.getRecordsBySchemaId(schemaId, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }
}
