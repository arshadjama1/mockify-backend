package com.mockify.backend.dto.request.project;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateProjectRequest {
    private String name;
    private UUID organizationId;
}
