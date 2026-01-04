package com.workforce.workforceplanning.workflow;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.model.ProjectStatus;
import com.workforce.workforceplanning.repository.ProjectRepository;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("projectCompletionDelegate")
public class ProjectCompletionDelegate implements JavaDelegate {

    @Autowired
    private ProjectRepository projectRepository;

    @Override
    public void execute(DelegateExecution execution) {
        Object projectIdObj = execution.getVariable("projectId");

        if (projectIdObj == null) {
            throw new RuntimeException("❌ projectId missing");
        }

        Long projectId;
        if (projectIdObj instanceof Number) {
            projectId = ((Number) projectIdObj).longValue();
        } else {
            projectId = Long.parseLong(projectIdObj.toString());
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("❌ Project not found"));

        project.setStatus(ProjectStatus.COMPLETED);
        projectRepository.save(project);

        execution.setVariable("allEmployeesConfirmed", true);
    }
}