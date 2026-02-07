package com.mockify.backend.repository;

import com.mockify.backend.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApiKeyRepository
        extends JpaRepository<ApiKey, UUID> {
}
