package com.mockify.backend.dto.request.member;

import com.mockify.backend.common.enums.MemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ChangeMemberRoleRequest {
    @NotNull
    private MemberRole role;
}