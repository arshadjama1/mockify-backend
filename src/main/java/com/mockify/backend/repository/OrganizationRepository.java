package com.mockify.backend.repository;

import com.mockify.backend.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    // Find all organizations owned by a user
    List<Organization> findByOwnerId(Long ownerId);

    // Find organization with owner and projects
    @Query("SELECT DISTINCT o FROM Organization o " +
            "JOIN FETCH o.owner " +
            "LEFT JOIN FETCH o.projects " +
            "WHERE o.id = :id")
    Optional<Organization> findByIdWithOwnerAndProjects(@Param("id") Long id);

    // Check if organization exists by name
    boolean existsByName(String name);

    // Delete by owner
    void deleteByOwnerId(Long ownerId);

    // Count all organizations
    long count();
}