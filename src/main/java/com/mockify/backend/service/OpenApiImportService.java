package com.mockify.backend.service;

import com.mockify.backend.dto.response.imports.OpenApiImportResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface OpenApiImportService {

    public OpenApiImportResponse importOpenApi(UUID userId, UUID projectId, MultipartFile file);
}
