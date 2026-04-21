package com.mockify.backend.common.enums;

public enum MemberRole {
    VIEWER,
    DEVELOPER,
    ADMIN,
    OWNER;

    /** True if this role can manage (invite/remove/change role of) a member with targetRole. */
    public boolean canManage(MemberRole target) {
        if (target == OWNER) return false;
        return ordinal() >= ADMIN.ordinal() && ordinal() > target.ordinal();
    }

    /** True if this role can invite someone at inviteeRole. */
    public boolean canInviteAs(MemberRole inviteeRole) {
        if (inviteeRole == OWNER) return false;
        return ordinal() >= ADMIN.ordinal() && ordinal() > inviteeRole.ordinal();
    }

    /** True if this role satisfies the minimum required role. */
    public boolean atLeast(MemberRole minimum) {
        return ordinal() >= minimum.ordinal();
    }
}