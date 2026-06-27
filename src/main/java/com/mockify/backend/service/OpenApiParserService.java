package com.mockify.backend.service;

import com.mockify.backend.dto.internal.ParsedOpenApiSpec;
import org.springframework.web.multipart.MultipartFile;


public interface OpenApiParserService {

    public ParsedOpenApiSpec parse(MultipartFile file);
}
