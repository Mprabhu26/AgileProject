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

@Component("projectApprovalDelegate")
@Transactional
public class ProjectApprovalDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ProjectApprovalDelegate.class);

    @Autowired
    private ProjectRepository projectRepository;

    @Override
    public void execute(DelegateExecution execution) {
        try {
            log.info("=== PROJECT APPROVAL DELEGATE STARTED ===");

            // Get decision from workflow
            Object approvedObj = execution.getVariable("approved");
            Object projectIdObj = execution.getVariable("projectId");

            log.info("Variables received:");
            log.info("  approved: {} (type: {})", approvedObj,
                    approvedObj != null ? approvedObj.getClass().getName() : "null");
            log.info("  projectId: {} (type: {})", projectIdObj,
                    projectIdObj != null ? projectIdObj.getClass().getName() : "null");

            if (approvedObj == null) {
                throw new RuntimeException("❌ 'approved' variable is null in workflow");
            }

            if (projectIdObj == null) {
                throw new RuntimeException("❌ 'projectId' variable is null in workflow");
            }

            // Parse project ID
            Long projectId;
            if (projectIdObj instanceof Number) {
                projectId = ((Number) projectIdObj).longValue();
            } else {
                projectId = Long.parseLong(projectIdObj.toString());
            }

            // Parse approval decision
            Boolean approved;
            if (approvedObj instanceof Boolean) {
                approved = (Boolean) approvedObj;
            } else {
                approved = Boolean.parseBoolean(approvedObj.toString());
            }

            log.info("🔄 Looking for project with ID: {}", projectId);

            // Find and update project
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("❌ Project not found with ID: " + projectId));

            log.info("📋 Project found: {} (ID: {})", project.getName(), project.getId());
            log.info("📋 BEFORE UPDATE - Status: {}, Workflow Status: {}",
                    project.getStatus(), project.getWorkflowStatus());

            // ✅ CRITICAL: Update BOTH status AND workflow_status in database
            if (Boolean.TRUE.equals(approved)) {
                project.setStatus(ProjectStatus.APPROVED);
                project.setWorkflowStatus("APPROVED");  // ← THIS UPDATES THE DATABASE COLUMN
                log.info("✅ Setting project status to APPROVED, workflow_status to 'APPROVED'");
            } else {
                project.setStatus(ProjectStatus.REJECTED);
                project.setWorkflowStatus("REJECTED");  // ← THIS UPDATES THE DATABASE COLUMN

                Object rejectionReason = execution.getVariable("rejectionReason");
                if (rejectionReason != null) {
                    log.info("Rejection reason: {}", rejectionReason);
                }

                log.info("❌ Setting project status to REJECTED, workflow_status to 'REJECTED'");
            }

            // ✅ Save to database with DEBUG logging
            log.info("💾 Saving project to database...");
            Project savedProject = projectRepository.save(project);
            projectRepository.flush(); // Force immediate write to database
            log.info("💾 Save completed. Saved project ID: {}", savedProject.getId());

            // Verify the save
            Project verifyProject = projectRepository.findById(projectId).orElse(null);
            if (verifyProject != null) {
                log.info("✅ VERIFIED - Status: {}, Workflow Status: {}",
                        verifyProject.getStatus(), verifyProject.getWorkflowStatus());
            }

            // Also set as Flowable process variables (for workflow logic)
            execution.setVariable("workflowStatus", project.getWorkflowStatus());
            execution.setVariable("projectStatus", project.getStatus().toString());
            execution.setVariable("processStatus", approved ? "DEPARTMENT_HEAD_APPROVED" : "DEPARTMENT_HEAD_REJECTED");
            execution.setVariable("statusUpdated", true);

            log.info("=== PROJECT APPROVAL DELEGATE COMPLETED ===");
            log.info("  Final project status: {}", project.getStatus());
            log.info("  Final workflow_status (database): {}", project.getWorkflowStatus());

        } catch (Exception e) {
            log.error("❌ CRITICAL ERROR in ProjectApprovalDelegate: {}", e.getMessage(), e);
            throw e;
        }
    }
}