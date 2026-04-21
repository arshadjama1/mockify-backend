package com.mockify.backend.common.enums;

import org.junit.jupiter.api.Test;
import static com.mockify.backend.common.enums.MemberRole.*;
import static org.junit.jupiter.api.Assertions.*;

class MemberRoleTest {

    // canManage
    @Test void owner_can_manage_admin()      { assertTrue(OWNER.canManage(ADMIN)); }
    @Test void owner_can_manage_developer()  { assertTrue(OWNER.canManage(DEVELOPER)); }
    @Test void admin_can_manage_developer()  { assertTrue(ADMIN.canManage(DEVELOPER)); }
    @Test void admin_can_manage_viewer()     { assertTrue(ADMIN.canManage(VIEWER)); }
    @Test void admin_cannot_manage_owner()   { assertFalse(ADMIN.canManage(OWNER)); }
    @Test void developer_cannot_manage_anyone() { assertFalse(DEVELOPER.canManage(VIEWER)); }
    @Test void nobody_can_manage_owner()     { assertFalse(ADMIN.canManage(OWNER)); assertFalse(OWNER.canManage(OWNER)); }

    // canInviteAs
    @Test void owner_can_invite_admin()      { assertTrue(OWNER.canInviteAs(ADMIN)); }
    @Test void admin_can_invite_developer()  { assertTrue(ADMIN.canInviteAs(DEVELOPER)); }
    @Test void admin_cannot_invite_admin()   { assertFalse(ADMIN.canInviteAs(ADMIN)); }
    @Test void nobody_can_invite_owner()     { assertFalse(ADMIN.canInviteAs(OWNER)); assertFalse(OWNER.canInviteAs(OWNER)); }

    // atLeast
    @Test void owner_atLeast_viewer()    { assertTrue(OWNER.atLeast(VIEWER)); }
    @Test void viewer_not_atLeast_admin(){ assertFalse(VIEWER.atLeast(ADMIN)); }
    @Test void admin_atLeast_admin()     { assertTrue(ADMIN.atLeast(ADMIN)); }
}