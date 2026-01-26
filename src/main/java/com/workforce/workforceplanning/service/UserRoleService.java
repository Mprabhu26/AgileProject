package com.workforce.workforceplanning.service;

import com.workforce.workforceplanning.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UserRoleService {

    private final UserRepository userRepository;

    public UserRoleService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get all Department Head usernames
     */
    @Transactional(readOnly = true)
    public List<String> getDepartmentHeadUsernames() {
        return userRepository.findUsernamesByRole("ROLE_DEPARTMENT_HEAD");
    }

    /**
     * Get all Resource Planner usernames
     */
    @Transactional(readOnly = true)
    public List<String> getResourcePlannerUsernames() {
        return userRepository.findUsernamesByRole("ROLE_RESOURCE_PLANNER");
    }

    /**
     * Get all Project Manager usernames
     */
    @Transactional(readOnly = true)
    public List<String> getProjectManagerUsernames() {
        return userRepository.findUsernamesByRole("ROLE_PROJECT_MANAGER");
    }

    /**
     * Get all usernames with a specific role
     */
    @Transactional(readOnly = true)
    public List<String> getUsernamesByRole(String role) {
        // Ensure role has ROLE_ prefix if not already present
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role.toUpperCase();
        }
        return userRepository.findUsernamesByRole(role);
    }

    /**
     * Check if user is a Department Head
     */
    @Transactional(readOnly = true)
    public boolean isUserDepartmentHead(String username) {
        return userRepository.findByUsername(username)
                .map(user -> "ROLE_DEPARTMENT_HEAD".equals(user.getRole()))
                .orElse(false);
    }
}