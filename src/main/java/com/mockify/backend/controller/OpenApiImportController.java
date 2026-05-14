package com.mockify.backend.controller;


import com.mockify.backend.dto.response.imports.OpenApiImportResponse;
import com.mockify.backend.security.SecurityUtils;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.OpenApiImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class OpenApiImportController {

    private final OpenApiImportService openApiImportService;
    private final EndpointService endpointService;

    @PostMapping(
            value = "/{org}/{project}/import/openapi",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<OpenApiImportResponse> importOpenApi(
            @PathVariable String org,
            @PathVariable String project,
            @RequestPart("file") MultipartFile file,
            Authentication auth
    ) {

        UUID userId = SecurityUtils.resolveUserId(auth);

        UUID projectId = endpointService.resolveProject(org, project);

        log.info("User {} importing OpenAPI file '{}' into org='{}', project='{}'",
                userId,
                file.getOriginalFilename(),
                org,
                project
        );

        OpenApiImportResponse response = openApiImportService.importOpenApi(userId, projectId, file);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
