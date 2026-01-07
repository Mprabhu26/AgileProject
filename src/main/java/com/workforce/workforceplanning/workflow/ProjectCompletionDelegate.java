package com.workforce.workforceplanning.workflow;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.model.ProjectStatus;
import com.workforce.workforceplanning.repository.ProjectRepository;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("projectCompletionDelegate")
@Transactional
public class ProjectCompletionDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ProjectCompletionDelegate.class);

    @Autowired
    private ProjectRepository projectRepository;

    @Override
    public void execute(DelegateExecution execution) {
        try {
            log.info("=== PROJECT COMPLETION DELEGATE STARTED ===");

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

            log.info("Completing project: {} (Current workflow_status: {})",
                    project.getName(), project.getWorkflowStatus());

            // ✅ CRITICAL: Update BOTH status AND workflow_status in database
            project.setStatus(ProjectStatus.COMPLETED);
            project.setWorkflowStatus("COMPLETED");  // ← UPDATE DATABASE COLUMN
            projectRepository.save(project);

            log.info("✅ Database updated: workflow_status = 'COMPLETED'");

            // Set workflow status variables
            execution.setVariable("workflowStatus", "COMPLETED");
            execution.setVariable("projectStatus", project.getStatus().toString());
            execution.setVariable("processStatus", "PROJECT_COMPLETED");
            execution.setVariable("allEmployeesConfirmed", true);

            log.info("=== PROJECT COMPLETION DELEGATE COMPLETED ===");

        } catch (Exception e) {
            log.error("❌ Error in project completion delegate", e);
            throw e;
        }
    }
}