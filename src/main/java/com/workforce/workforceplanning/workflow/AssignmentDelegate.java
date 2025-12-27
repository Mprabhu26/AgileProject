package com.workforce.workforceplanning.workflow;

import com.workforce.workforceplanning.model.Assignment;
import com.workforce.workforceplanning.model.AssignmentStatus;
import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.repository.AssignmentRepository;
import com.workforce.workforceplanning.repository.ProjectRepository;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("assignmentDelegate")
public class AssignmentDelegate implements JavaDelegate {

    private final AssignmentRepository assignmentRepository;
    private final ProjectRepository projectRepository;

    public AssignmentDelegate(AssignmentRepository assignmentRepository,
                              ProjectRepository projectRepository) {
        this.assignmentRepository = assignmentRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public void execute(DelegateExecution execution) {

        Object projectIdVar = execution.getVariable("projectId");

        if (projectIdVar == null) {
            throw new RuntimeException("projectId process variable is missing");
        }

        Long projectId;
        if (projectIdVar instanceof Number) {
            projectId = ((Number) projectIdVar).longValue();
        } else {
            projectId = Long.valueOf(projectIdVar.toString());
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        Assignment assignment = new Assignment();
        assignment.setProject(project);
        assignment.setStatus(AssignmentStatus.ASSIGNED);

        assignmentRepository.save(assignment);
    }
}
