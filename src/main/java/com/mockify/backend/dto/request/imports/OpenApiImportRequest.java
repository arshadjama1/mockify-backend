package com.mockify.backend.dto.request.imports;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenApiImportRequest {

    @NotNull(message = "OpenAPI file is required")
    private MultipartFile file;
}