package com.mockify.backend.repository;

import com.mockify.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Find user by email
    Optional<User> findByEmail(String email);

    // Check if email exists
    boolean existsByEmail(String email);

    // Delete user by email
    void deleteByEmail(String email);

    // Count all users
    long count();

    Optional<User> findByUsername(String username);

    Optional<User> findByProviderNameAndProviderId(String providerName, String providerId);

    boolean existsByUsername(String username);
}