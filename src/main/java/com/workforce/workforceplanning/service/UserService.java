package com.workforce.workforceplanning.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.workforce.workforceplanning.model.Project;

import java.util.Collection;

@Service
public class UserService {

    /**
     * Get current authenticated user's email/username
     */
    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    /**
     * Get current user's display name (for notifications)
     */
    public String getCurrentUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            // In a real app, you might fetch from user details
            // For now, use username or extract from principal
            return auth.getName(); // Or get from user details
        }
        return "Unknown User";
    }

    /**
     * Check if current user has a specific role
     */
    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            return auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority -> authority.equals("ROLE_" + role));
        }
        return false;
    }

    /**
     * Get user's role (first role found)
     */
    public String getUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            if (!authorities.isEmpty()) {
                String role = authorities.iterator().next().getAuthority();
                // Remove "ROLE_" prefix if present
                return role.startsWith("ROLE_") ? role.substring(5) : role;
            }
        }
        return "USER";
    }

    /**
     * Check if current user is Project Manager (creator of project)
     */
    public boolean isProjectManager(Project project) {
        String currentUser = getCurrentUserEmail();
        return project.getCreatedBy().equals(currentUser);
    }

    /**
     * Check if current user is Department Head
     */
    public boolean isDepartmentHead() {
        return hasRole("DEPARTMENT_HEAD");
    }

    /**
     * Check if current user is Resource Planner
     */
    public boolean isResourcePlanner() {
        return hasRole("RESOURCE_PLANNER");
    }
}