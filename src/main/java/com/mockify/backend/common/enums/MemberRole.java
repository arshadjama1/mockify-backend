package com.mockify.backend.common.enums;

public enum MemberRole {
    VIEWER(0),
    DEVELOPER(1),
    ADMIN(2),
    OWNER(3);

    private final int level;

    MemberRole(int level) {
        this.level = level;
    }

    /** True if this role satisfies the minimum required role. */
    public boolean atLeast(MemberRole other) {
        return this.level >= other.level;
    }

    public boolean higherThan(MemberRole other) {
        return this.level > other.level;
    }

    /** True if this role can manage (invite/remove/change role of) a member with targetRole. */
    public boolean canManage(MemberRole target) {
        if (target == OWNER) return false;
        return this.atLeast(ADMIN) && this.higherThan(target);
    }

    /** True if this role can invite someone at inviteeRole. */
    public boolean canInviteAs(MemberRole inviteeRole) {
        if (inviteeRole == OWNER) return false;
        return this.atLeast(ADMIN) && this.higherThan(inviteeRole);
    }
}