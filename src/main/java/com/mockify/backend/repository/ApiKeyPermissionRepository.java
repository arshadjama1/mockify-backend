package com.mockify.backend.repository;

import com.mockify.backend.model.ApiKeyPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApiKeyPermissionRepository
        extends JpaRepository<ApiKeyPermission, UUID> {
}
