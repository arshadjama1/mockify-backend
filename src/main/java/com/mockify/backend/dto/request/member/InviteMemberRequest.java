package com.mockify.backend.dto.request.member;

import com.mockify.backend.common.enums.MemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteMemberRequest {
    @NotBlank @Email
    private String email;

    @NotNull
    private MemberRole role;
}