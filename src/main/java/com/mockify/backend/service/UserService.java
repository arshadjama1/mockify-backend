package com.mockify.backend.service;

import com.mockify.backend.dto.response.auth.UserResponse;

import java.util.List;
import java.util.UUID;

//Manages users (typically admin-level operations).
public interface UserService {

    // Fetch user details by ID
    UserResponse getUserById(UUID id);

    // Fetch user details by email
    UserResponse getUserByEmail(String email);

    // Get list of all users (admin use only)
    List<UserResponse> getAllUsers();

    // Delete user by ID
    void deleteUser(UUID id);

    // Check if email already exists
    boolean existsByEmail(String email);

    // Total registered users
    long countUsers();

}
