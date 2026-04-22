package com.mockify.backend.dto.response.member;

import com.mockify.backend.common.enums.MemberRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvitationResponse {
    private UUID id;
    private String email;
    private MemberRole role;
    private String invitedByName;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}