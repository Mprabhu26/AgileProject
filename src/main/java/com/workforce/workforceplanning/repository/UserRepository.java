package com.workforce.workforceplanning.repository;

import com.workforce.workforceplanning.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    // Add these methods for role-based queries
    List<User> findByRole(String role);

    @Query("SELECT u.username FROM User u WHERE u.role = :role")
    List<String> findUsernamesByRole(@Param("role") String role);

    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findAllByRole(@Param("role") String role);
}