package com.mockify.backend.dto.request.imports;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;


public class OpenApiImportRequest {

    @NotNull(message = "OpenAPI file is required")
    private MultipartFile file;
}