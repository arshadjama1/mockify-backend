package com.mockify.backend.dto.internal;

import io.swagger.v3.oas.models.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@AllArgsConstructor
@Getter @Setter
public class ParsedOpenApiSpec {
    private String version;
    private Map<String, Schema> schemas;
}