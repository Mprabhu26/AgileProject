package com.workforce.workforceplanning.workflow;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.model.ProjectStatus;
import com.workforce.workforceplanning.repository.ProjectRepository;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("projectCompletionDelegate")
public class ProjectCompletionDelegate implements JavaDelegate {

    private final ProjectRepository projectRepository;

    public ProjectCompletionDelegate(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public void execute(DelegateExecution execution) {

        Long projectId = (Long) execution.getVariable("projectId");

        if (projectId == null) {
            throw new RuntimeException("❌ projectId missing");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("❌ Project not found"));

        project.setStatus(ProjectStatus.COMPLETED);
        projectRepository.save(project);

        execution.setVariable("allEmployeesConfirmed", true);
    }
}
