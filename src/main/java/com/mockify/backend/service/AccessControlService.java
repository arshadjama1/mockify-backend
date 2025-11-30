package com.mockify.backend.service;

import com.mockify.backend.model.Organization;

import java.util.UUID;

public interface AccessControlService {

    void checkOrganizationAccess(UUID userId, Organization organization, String resourceName);
}
