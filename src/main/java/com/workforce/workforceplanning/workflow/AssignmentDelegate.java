package com.workforce.workforceplanning.workflow;

import com.workforce.workforceplanning.model.*;
import com.workforce.workforceplanning.repository.*;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component("assignmentDelegate")
public class AssignmentDelegate implements JavaDelegate {

    private final AssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;

    public AssignmentDelegate(
            AssignmentRepository assignmentRepository,
            EmployeeRepository employeeRepository,
            ProjectRepository projectRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public void execute(DelegateExecution execution) {

        Long projectId = ((Number) execution.getVariable("projectId")).longValue();
        List<?> employeeIdsRaw = (List<?>) execution.getVariable("employeeIds");

        if (employeeIdsRaw == null || employeeIdsRaw.isEmpty()) {
            throw new RuntimeException("❌ employeeIds missing");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("❌ Project not found"));

        for (Object idObj : employeeIdsRaw) {
            Long employeeId = ((Number) idObj).longValue();

            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("❌ Employee not found: " + employeeId));

            // 🔹 Create Assignment
            Assignment assignment = new Assignment();
            assignment.setProject(project);
            assignment.setEmployee(employee);
            assignment.setStatus(AssignmentStatus.ASSIGNED);
            assignment.setAssignedAt(LocalDateTime.now());

            assignmentRepository.save(assignment);

            // 🔹 Mark employee unavailable
            employee.setAvailable(false);
            employeeRepository.save(employee);
        }

        // 🔹 Update project status
        project.setStatus(ProjectStatus.IN_PROGRESS);
        projectRepository.save(project);
    }
}
