package com.workforce.workforceplanning.controller;

import com.workforce.workforceplanning.model.Assignment;
import com.workforce.workforceplanning.model.AssignmentStatus;  // ← ADD THIS LINE
import com.workforce.workforceplanning.model.Employee;
import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.repository.AssignmentRepository;
import com.workforce.workforceplanning.repository.EmployeeRepository;
import com.workforce.workforceplanning.repository.ProjectRepository;
import com.workforce.workforceplanning.service.AssignmentValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;  // ← ALSO ADD THIS IF MISSING
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Handle employee-project assignments
 */
@RestController
@RequestMapping("/assignments")
public class AssignmentController {

    private final AssignmentRepository assignmentRepository;
    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;

    // ✅ Add validation service
    @Autowired
    private AssignmentValidationService validationService;

    public AssignmentController(AssignmentRepository assignmentRepository,
                                ProjectRepository projectRepository,
                                EmployeeRepository employeeRepository) {
        this.assignmentRepository = assignmentRepository;
        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
    }

    /**
     * GET ALL ASSIGNMENTS
     * GET /assignments
     */
    @GetMapping
    public ResponseEntity<List<Assignment>> getAllAssignments() {
        return ResponseEntity.ok(assignmentRepository.findAll());
    }

    /**
     * CREATE ASSIGNMENT (Assign employee to project)
     * POST /assignments
     * Body: {"projectId": 1, "employeeId": 2}
     */
    @PostMapping
    public ResponseEntity<?> createAssignment(@RequestBody Map<String, Long> request) {

        Long projectId = request.get("projectId");
        Long employeeId = request.get("employeeId");

        var projectOpt = projectRepository.findById(projectId);
        var employeeOpt = employeeRepository.findById(employeeId);

        if (projectOpt.isEmpty() || employeeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project or Employee not found"));
        }

        Project project = projectOpt.get();
        Employee employee = employeeOpt.get();

        // ✅ USE VALIDATION SERVICE
        AssignmentValidationService.ValidationResult validation =
                validationService.canAssignEmployeeToProject(projectId, employeeId);

        if (validation.isError()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", validation.getMessage()));
        }

        Assignment assignment = new Assignment(project, employee, AssignmentStatus.ASSIGNED);
        Assignment saved = assignmentRepository.save(assignment);

        // ✅ Update employee availability
        employee.setAvailable(false);
        employeeRepository.save(employee);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * GET ASSIGNMENTS BY PROJECT
     * GET /assignments/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Assignment>> getByProject(@PathVariable Long projectId) {
        List<Assignment> assignments = assignmentRepository.findAll().stream()
                .filter(a -> a.getProject().getId().equals(projectId))
                .toList();
        return ResponseEntity.ok(assignments);
    }

    /**
     * GET ASSIGNMENTS BY EMPLOYEE
     * GET /assignments/employee/{employeeId}
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<Assignment>> getByEmployee(@PathVariable Long employeeId) {
        List<Assignment> assignments = assignmentRepository.findAll().stream()
                .filter(a -> a.getEmployee().getId().equals(employeeId))
                .toList();
        return ResponseEntity.ok(assignments);
    }

    /**
     * UPDATE ASSIGNMENT STATUS
     * PUT /assignments/{id}/status
     * Body: {"status": "IN_PROGRESS"}
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        var assignmentOpt = assignmentRepository.findById(id);
        if (assignmentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Assignment assignment = assignmentOpt.get();
        String statusStr = request.get("status");

        try {
            AssignmentStatus status = AssignmentStatus.valueOf(statusStr.toUpperCase());
            assignment.setStatus(status);
            Assignment updated = assignmentRepository.save(assignment);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid status: " + statusStr));
        }
    }

    /**
     * DELETE ASSIGNMENT
     * DELETE /assignments/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAssignment(@PathVariable Long id) {
        if (!assignmentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        assignmentRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Assignment deleted"));
    }

    /**
     * APPROVE ASSIGNMENT (Department Head action)
     * PUT /assignments/{id}/approve
     * This approves the resource planner's proposal
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveAssignment(@PathVariable Long id) {
        var assignmentOpt = assignmentRepository.findById(id);

        if (assignmentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Assignment assignment = assignmentOpt.get();

        // Validate current status
        if (assignment.getStatus() != AssignmentStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Can only approve PENDING assignments",
                    "currentStatus", assignment.getStatus()
            ));
        }

        assignment.setStatus(AssignmentStatus.APPROVED);
        Assignment updated = assignmentRepository.save(assignment);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Assignment approved by Department Head",
                "assignmentId", updated.getId(),
                "employeeId", updated.getEmployee().getId(),
                "projectId", updated.getProject().getId(),
                "status", updated.getStatus().toString()
        ));
    }

    /**
     * REJECT ASSIGNMENT (Department Head action)
     * PUT /assignments/{id}/reject
     * Body: {"reason": "Employee overbooked"} (optional)
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectAssignment(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request) {

        var assignmentOpt = assignmentRepository.findById(id);

        if (assignmentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Assignment assignment = assignmentOpt.get();

        // Validate current status
        if (assignment.getStatus() != AssignmentStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Can only reject PENDING assignments",
                    "currentStatus", assignment.getStatus()
            ));
        }

        assignment.setStatus(AssignmentStatus.REJECTED);
        Assignment updated = assignmentRepository.save(assignment);

        String reason = (request != null && request.containsKey("reason"))
                ? request.get("reason")
                : "No reason provided";

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Assignment rejected by Department Head",
                "assignmentId", updated.getId(),
                "employeeId", updated.getEmployee().getId(),
                "projectId", updated.getProject().getId(),
                "status", updated.getStatus().toString(),
                "reason", reason
        ));
    }

    /**
     * EMPLOYEE CONFIRMS ASSIGNMENT
     * PUT /assignments/{id}/confirm
     * This is the final confirmation by the employee
     * Marks employee as unavailable
     */
    @PutMapping("/{id}/confirm")
    @Transactional  // ← ADD THIS if not already present
    public ResponseEntity<?> confirmAssignment(@PathVariable Long id) {
        var assignmentOpt = assignmentRepository.findById(id);

        if (assignmentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Assignment assignment = assignmentOpt.get();

        // Validate: Can only confirm APPROVED assignments
        if (assignment.getStatus() != AssignmentStatus.APPROVED) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Can only confirm APPROVED assignments",
                    "currentStatus", assignment.getStatus()
            ));
        }

        // Update assignment status
        assignment.setStatus(AssignmentStatus.CONFIRMED);

        // CRITICAL: Mark employee as unavailable
        Employee employee = assignment.getEmployee();
        if (employee != null) {
            employee.setAvailable(false);
            employeeRepository.save(employee);
        }

        Assignment updated = assignmentRepository.save(assignment);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Assignment confirmed by employee",
                "assignmentId", updated.getId(),
                "employeeId", updated.getEmployee().getId(),
                "employeeName", updated.getEmployee().getName(),
                "projectId", updated.getProject().getId(),
                "projectName", updated.getProject().getName(),
                "status", updated.getStatus().toString(),
                "employeeAvailable", employee.getAvailable()
        ));
    }
}