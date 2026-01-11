package com.workforce.workforceplanning.workflow;

import com.workforce.workforceplanning.model.*;
import com.workforce.workforceplanning.repository.*;
import com.workforce.workforceplanning.service.AssignmentValidationService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Arrays;

@Component("assignmentDelegate")
public class AssignmentDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(AssignmentDelegate.class);

    private final AssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;

    // List of statuses that indicate an employee is actively assigned
    private static final List<AssignmentStatus> ACTIVE_STATUSES = Arrays.asList(
            AssignmentStatus.PENDING,
            AssignmentStatus.ASSIGNED,
            AssignmentStatus.IN_PROGRESS
    );

    // ✅ Add validation service
    @Autowired
    private AssignmentValidationService validationService;

    // ✅ FIXED: Constructor injection
    public AssignmentDelegate(
            AssignmentRepository assignmentRepository,
            EmployeeRepository employeeRepository,
            ProjectRepository projectRepository) {
        this.assignmentRepository = assignmentRepository;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
        log.info("✅ AssignmentDelegate initialized with repositories");
    }

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        log.info("=== ASSIGNING EMPLOYEES TO PROJECT ===");

        try {
            // ✅ Check if repositories are initialized
            if (assignmentRepository == null || employeeRepository == null || projectRepository == null) {
                log.error("❌ One or more repositories are null in AssignmentDelegate!");
                throw new RuntimeException("Repositories not initialized in AssignmentDelegate");
            }

            // 1. Get project ID from workflow
            Object projectIdObj = execution.getVariable("projectId");
            Long projectId = parseLong(projectIdObj);
            log.info("Project ID from workflow: {}", projectId);

            // 2. Get approved employee IDs
            Object employeeIdsObj = execution.getVariable("proposedEmployeeIds");
            List<Long> employeeIds = parseEmployeeIds(employeeIdsObj);
            log.info("Employee IDs from workflow: {}", employeeIds);

            // 3. Get project from database
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> {
                        log.error("❌ Project not found in database: {}", projectId);
                        return new RuntimeException("Project not found: " + projectId);
                    });
            log.info("Found project: {}", project.getName());

            // 4. Create assignments for each approved employee
            int assignedCount = 0;
            int skippedCount = 0;

            for (Long employeeId : employeeIds) {
                Employee employee = employeeRepository.findById(employeeId)
                        .orElseThrow(() -> {
                            log.error("❌ Employee not found: {}", employeeId);
                            return new RuntimeException("Employee not found: " + employeeId);
                        });

                // Check if already assigned
                boolean alreadyAssigned = assignmentRepository.findAll().stream()
                        .anyMatch(a -> a.getProject() != null &&
                                a.getProject().getId().equals(projectId) &&
                                a.getEmployee() != null &&
                                a.getEmployee().getId().equals(employeeId));

                if (!alreadyAssigned) {
                    // Create PENDING assignment (awaiting employee confirmation)
                    Assignment assignment = new Assignment(project, employee, AssignmentStatus.PENDING);
                    assignmentRepository.save(assignment);



                    assignedCount++;
                    log.info("✅ Assigned employee {} to project {}", employee.getName(), project.getName());
                } else {
                    log.info("⚠️ Employee {} already assigned to project {}", employee.getName(), project.getName());
                }

                // Only assign if validation passed
                Assignment assignment = new Assignment(project, employee, AssignmentStatus.ASSIGNED);
                assignmentRepository.save(assignment);

                // Update employee availability
                employee.setAvailable(false);
                employeeRepository.save(employee);

                assignedCount++;
                log.info("✅ Assigned employee {} to project {}", employee.getName(), project.getName());
            }

            // 5. Update project status
            project.setStatus(ProjectStatus.IN_PROGRESS);
            projectRepository.save(project);

            log.info("✅ Successfully assigned {} employees, skipped {}. Project {} moved to IN_PROGRESS",
                    assignedCount, skippedCount, project.getName());

            // Set workflow variables for reporting
            execution.setVariable("assignedCount", assignedCount);
            execution.setVariable("skippedCount", skippedCount);

        } catch (Exception e) {
            log.error("❌ Failed to assign employees", e);
            throw e;
        }
    }

    /**
     * Check if employee is already assigned to the project with an active status
     * (Keep as backup or remove if using validation service)
     */
    private boolean isEmployeeAlreadyAssigned(Long projectId, Long employeeId) {
        return assignmentRepository.findAll().stream()
                .anyMatch(a -> a.getProject() != null &&
                        a.getProject().getId().equals(projectId) &&
                        a.getEmployee() != null &&
                        a.getEmployee().getId().equals(employeeId) &&
                        ACTIVE_STATUSES.contains(a.getStatus()));
    }

    private Long parseLong(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        } else if (obj != null) {
            try {
                return Long.parseLong(obj.toString());
            } catch (NumberFormatException e) {
                log.error("❌ Cannot parse projectId: {}", obj);
                throw new RuntimeException("Invalid projectId: " + obj);
            }
        }
        throw new RuntimeException("Project ID is null");
    }

    @SuppressWarnings("unchecked")
    private List<Long> parseEmployeeIds(Object obj) {
        if (obj instanceof List) {
            List<?> rawList = (List<?>) obj;
            return rawList.stream()
                    .map(item -> {
                        if (item instanceof Number) {
                            return ((Number) item).longValue();
                        } else if (item != null) {
                            return Long.parseLong(item.toString());
                        }
                        return null;
                    })
                    .filter(id -> id != null)
                    .toList();
        } else if (obj != null) {
            log.warn("⚠️ employeeIds is not a List, type: {}", obj.getClass().getName());
        }
        log.info("⚠️ No employeeIds found in workflow variables");
        return List.of();
    }
}