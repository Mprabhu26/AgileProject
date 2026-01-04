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

@Component("projectApprovalDelegate")  // Must match bean name
public class ProjectApprovalDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ProjectApprovalDelegate.class);

    @Autowired
    private ProjectRepository projectRepository;

    // CRITICAL: Add empty constructor
    public ProjectApprovalDelegate() {
        log.info("ProjectApprovalDelegate constructor called");
    }

    @Override
    public void execute(DelegateExecution execution) {
        try {
            log.info("ProjectApprovalDelegate: Processing approval decision...");

            // Just log - don't try to update database
            Object approved = execution.getVariable("approved");
            Object projectId = execution.getVariable("projectId");

            log.info("Decision: approved={}, projectId={}", approved, projectId);
            log.info("Note: Project status already updated in controller");

        } catch (Exception e) {
            log.warn("Non-critical error in delegate: {}", e.getMessage());
            // Don't throw - allow workflow to continue
        }
    }
}