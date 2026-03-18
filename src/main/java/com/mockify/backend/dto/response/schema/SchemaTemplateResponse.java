
package com.mockify.backend.dto.response.schema;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class SchemaTemplateResponse {

    private UUID id;

    private String name;
    private String slug;
    private String description;

    private boolean systemTemplate;

    // Organization info (null for system templates)
    private UUID organizationId;
    private String organizationName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
