package com.mockify.backend.service;

import com.mockify.backend.dto.internal.ParsedOpenApiSpec;

public interface OpenApiValidatorService {

    public void validate(ParsedOpenApiSpec spec);
}
