package com.workforce.workforceplanning.workflow;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("projectStartDelegate")
@Transactional
public class ProjectStartDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ProjectStartDelegate.class);

    // This won't work directly with Flowable - we need to get it from Spring context
    private static com.workforce.workforceplanning.repository.ProjectRepository projectRepository;

    // Use setter injection instead
    @Autowired
    public void setProjectRepository(com.workforce.workforceplanning.repository.ProjectRepository projectRepository) {
        ProjectStartDelegate.projectRepository = projectRepository;
    }

    @Override
    public void execute(DelegateExecution execution) {
        log.info("=== PROJECT WORKFLOW STARTED ===");

        try {
            Object projectIdObj = execution.getVariable("projectId");
            Object projectNameObj = execution.getVariable("projectName");

            log.info("projectIdObj type: {}", projectIdObj != null ? projectIdObj.getClass().getName() : "null");
            log.info("projectIdObj value: {}", projectIdObj);

            if (projectIdObj == null) {
                throw new RuntimeException("projectId is null in workflow variables");
            }

            Long projectId;
            if (projectIdObj instanceof Number) {
                projectId = ((Number) projectIdObj).longValue();
            } else {
                projectId = Long.parseLong(projectIdObj.toString());
            }

            String projectName = projectNameObj != null ? projectNameObj.toString() : "Unknown";

            // Check if repository is available
            if (projectRepository == null) {
                log.warn("⚠️ ProjectRepository is null - cannot update database");
                // Still set variables for workflow
                execution.setVariable("workflowStatus", "RUNNING");
                execution.setVariable("processStatus", "AWAITING_DEPARTMENT_HEAD_APPROVAL");
                execution.setVariable("projectStatus", "PENDING");
                log.info("✅ Workflow variables set (database not updated)");
                return;
            }

            // Find and update project
            var project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

            // Update the database column
            project.setWorkflowStatus("RUNNING");
            project.setProcessInstanceId(execution.getProcessInstanceId());
            projectRepository.save(project);

            log.info("✅ Database updated: project {} (ID: {}) workflow_status = 'RUNNING'",
                    projectName, projectId);

            // Set initial workflow status as Flowable process variables
            execution.setVariable("workflowStatus", "RUNNING");
            execution.setVariable("processStatus", "AWAITING_DEPARTMENT_HEAD_APPROVAL");
            execution.setVariable("projectStatus", "PENDING");

            log.info("=== PROJECT WORKFLOW STARTED SUCCESSFULLY ===");

        } catch (Exception e) {
            log.error("❌ Error in project start delegate", e);
            throw e;
        }
    }
}