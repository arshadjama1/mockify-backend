package com.mockify.backend.dto.response.organization;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationResponse {
    private UUID id;
    private String name;
    private String slug;
    private UUID ownerId;
    private String ownerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int projectCount;
}
