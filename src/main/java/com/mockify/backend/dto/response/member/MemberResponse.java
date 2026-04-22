package com.mockify.backend.dto.response.member;

import com.mockify.backend.common.enums.MemberRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponse {
    private UUID id;
    private UUID userId;
    private String name;
    private String email;
    private String avatarUrl;
    private MemberRole role;
    private LocalDateTime joinedAt;
}