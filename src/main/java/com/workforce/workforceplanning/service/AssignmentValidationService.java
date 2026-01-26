package com.workforce.workforceplanning.service;

import com.workforce.workforceplanning.model.*;
import com.workforce.workforceplanning.repository.AssignmentRepository;
import com.workforce.workforceplanning.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class AssignmentValidationService {

    private final AssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;

    // Statuses that mean "employee is actively assigned"
    private static final List<AssignmentStatus> ACTIVE_STATUSES = Arrays.asList(
            AssignmentStatus.PENDING,
            AssignmentStatus.ASSIGNED,
            AssignmentStatus.IN_PROGRESS
    );

    public AssignmentValidationService(AssignmentRepository assignmentRepository,
                                       EmployeeRepository employeeRepository) {
        this.assignmentRepository = assignmentRepository;
        this.employeeRepository = employeeRepository;
    }

    /**
     * MAIN VALIDATION METHOD - Use this everywhere
     * Returns true if employee can be assigned, false if not (with reason)
     */
    public ValidationResult canAssignEmployeeToProject(Long projectId, Long employeeId) {
        // 1. Check if employee exists
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            return ValidationResult.error("Employee not found");
        }

        // 2. Check if employee is available
        if (!Boolean.TRUE.equals(employee.getAvailable())) {
            return ValidationResult.error("Employee is not available");
        }

        // 3. Check if already assigned to this project
        if (isEmployeeAlreadyAssigned(projectId, employeeId)) {
            return ValidationResult.error("Employee already assigned to this project");
        }

        return ValidationResult.success();
    }

    /**
     * Check if employee is already assigned (any active status)
     */
    public boolean isEmployeeAlreadyAssigned(Long projectId, Long employeeId) {
        return assignmentRepository.findAll().stream()
                .anyMatch(a -> a.getProject() != null &&
                        a.getProject().getId().equals(projectId) &&
                        a.getEmployee() != null &&
                        a.getEmployee().getId().equals(employeeId) &&
                        ACTIVE_STATUSES.contains(a.getStatus()));
    }

    /**
     * Assign employee and update availability (one call does everything)
     */
    @Transactional
    public Assignment assignEmployee(Long projectId, Long employeeId, AssignmentStatus status) {
        // Validate first
        ValidationResult validation = canAssignEmployeeToProject(projectId, employeeId);
        if (!validation.isSuccess()) {
            throw new RuntimeException(validation.getMessage());
        }

        // Get employee and update availability
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        employee.setAvailable(false);
        employeeRepository.save(employee);

        // Create and return assignment (you need to fetch project too)
        // Note: You'll need to inject ProjectRepository or pass Project object
        return null; // Placeholder - you'll need to implement this based on your needs
    }

    /**
     * Simple validation result class
     */
    public static class ValidationResult {
        private final boolean success;
        private final String message;

        private ValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, "Validation passed");
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isSuccess() { return success; }
        public boolean isError() { return !success; }
        public String getMessage() { return message; }
    }
}