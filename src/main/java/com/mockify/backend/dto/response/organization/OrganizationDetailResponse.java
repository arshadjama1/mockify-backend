package com.mockify.backend.dto.response.organization;

import com.mockify.backend.dto.response.auth.UserResponse;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationDetailResponse {
    private UUID id;
    private String name;
    private UserResponse owner;
    private LocalDateTime createdAt;
    private List<ProjectSummary> projects;

    @Data
    public static class ProjectSummary {
        private UUID id;
        private String name;
        private int schemaCount;
        private LocalDateTime createdAt;
    }
}