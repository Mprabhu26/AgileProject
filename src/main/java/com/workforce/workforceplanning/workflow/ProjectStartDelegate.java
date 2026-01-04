package com.workforce.workforceplanning.workflow;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.model.ProjectStatus;
import com.workforce.workforceplanning.repository.ProjectRepository;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("projectStartDelegate")
public class ProjectStartDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ProjectStartDelegate.class);

    private final ProjectRepository projectRepository;

    public ProjectStartDelegate(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public void execute(DelegateExecution execution) {
        log.info("=== PROJECT WORKFLOW STARTED ===");

        try {
            // Get project ID from workflow variables
            Long projectId = (Long) execution.getVariable("projectId");
            String projectName = (String) execution.getVariable("projectName");

            log.info("Project: {} (ID: {})", projectName, projectId);
            log.info("Workflow Instance ID: {}", execution.getProcessInstanceId());

            // Find project in database
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

            // Update project workflow status
            project.setWorkflowStatus("IN_PROGRESS");
            projectRepository.save(project);

            // Log all workflow variables for debugging
            log.info("Workflow Variables:");
            execution.getVariables().forEach((key, value) -> {
                log.info("  {} = {}", key, value);
            });

            log.info("=== PROJECT WORKFLOW STARTED SUCCESSFULLY ===");

        } catch (Exception e) {
            log.error("❌ Error in project start delegate", e);
            throw e; // Re-throw to let Flowable handle the error
        }
    }
}