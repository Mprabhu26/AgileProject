package com.workforce.workforceplanning.workflow;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component; // ADD THIS

@Component("projectStartDelegate") // ADD THIS
public class ProjectStartDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ProjectStartDelegate.class);

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

            log.info("✅ ProjectStartDelegate executed for project: {} (ID: {})", projectName, projectId);
            log.info("Workflow Instance ID: {}", execution.getProcessInstanceId());

            log.info("=== PROJECT WORKFLOW STARTED SUCCESSFULLY ===");

        } catch (Exception e) {
            log.error("❌ Error in project start delegate", e);
            throw e;
        }
    }
}