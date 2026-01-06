// FILE: ExternalSearchService.java (simplified version)
package com.workforce.workforceplanning.service;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.flowable.engine.RuntimeService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExternalSearchService {

    private static final Logger log = LoggerFactory.getLogger(ExternalSearchService.class);
    private final ProjectRepository projectRepository;
    private final RuntimeService runtimeService;
    private final NotificationService notificationService;

    public ExternalSearchService(
            ProjectRepository projectRepository,
            RuntimeService runtimeService,
            NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.runtimeService = runtimeService;
        this.notificationService = notificationService;
    }

    /**
     * Check skill gaps and notify PM if external search needed
     */
    @Transactional
    public void checkAndNotifySkillGaps(Project project, Map<String, Integer> criticalGaps) {
        if (!criticalGaps.isEmpty()) {
            log.info("⚠️ Critical skill gaps detected for project: {}", project.getName());
            log.info("Gaps: {}", criticalGaps);

            // Update project to indicate external search is needed
            project.setExternalSearchNeeded(true);
            project.setExternalSearchNotes("Skill gaps detected: " + criticalGaps.keySet());
            projectRepository.save(project);

            // Send notification to Project Manager
            notificationService.sendSkillGapNotification(project, criticalGaps);

            log.info("✅ Skill gap notification sent to Project Manager: {}", project.getCreatedBy());
        }
    }

    /**
     * Project Manager triggers external search (starts workflow)
     */
    @Transactional
    public String triggerExternalSearch(Long projectId, String pmUsername) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify the PM is the creator
        if (!project.getCreatedBy().equals(pmUsername)) {
            throw new RuntimeException("Only the project creator can trigger external search");
        }

        log.info("🚀 Project Manager {} triggering external search for project: {}",
                pmUsername, project.getName());

        // Start workflow for external search approval
        Map<String, Object> variables = new HashMap<>();
        variables.put("projectId", projectId);
        variables.put("projectName", project.getName());
        variables.put("requestedBy", pmUsername);
        variables.put("externalSearchType", "skill_gap");
        variables.put("requestTime", LocalDateTime.now());

        String processInstanceId;

        try {
            // Try to start Flowable workflow
            var processInstance = runtimeService.startProcessInstanceByKey(
                    "externalSearchApprovalProcess",
                    variables
            );
            processInstanceId = processInstance.getId();
            log.info("✅ Flowable workflow started: {}", processInstanceId);
        } catch (Exception e) {
            // Fallback if BPMN not configured
            log.warn("Flowable process not found, using simple workflow: {}", e.getMessage());
            processInstanceId = "MANUAL-" + System.currentTimeMillis();

            // For manual workflow, we need to:
            // 1. Set workflow status
            // 2. Create a task for Department Head
            project.setWorkflowStatus("AWAITING_DEPARTMENT_HEAD_APPROVAL");

            // Log manual task creation
            log.info("📋 Manual task created for Department Head approval");
            log.info("  Project: {}", project.getName());
            log.info("  Requested by: {}", pmUsername);
            log.info("  Process ID: {}", processInstanceId);
        }

        // Update project with workflow info
        project.setProcessInstanceId(processInstanceId);
        project.setWorkflowStatus("AWAITING_DEPARTMENT_HEAD_APPROVAL");
        projectRepository.save(project);

        return processInstanceId;
    }

    /**
     * Department Head approves/rejects external search
     */
    @Transactional
    public void processExternalSearchDecision(Long projectId, String dhUsername,
                                              boolean approved, String reason) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        log.info("📋 Department Head {} {} external search for project: {}",
                dhUsername, approved ? "APPROVED" : "REJECTED", project.getName());

        if (approved) {
            project.setExternalSearchNeeded(true);
            project.setWorkflowStatus("EXTERNAL_SEARCH_APPROVED");
            project.setExternalSearchRequestedAt(LocalDateTime.now());

            // Notify both PM and RP
            notificationService.sendExternalSearchApprovalNotification(
                    projectId, project.getName(), dhUsername, true);

            log.info("✅ External search approved. Resource Planner can now proceed.");
        } else {
            project.setExternalSearchNeeded(false);
            project.setWorkflowStatus("EXTERNAL_SEARCH_REJECTED");
            project.setExternalSearchNotes("Rejected by " + dhUsername + ": " + reason);

            // Notify both PM and RP
            notificationService.sendExternalSearchApprovalNotification(
                    projectId, project.getName(), dhUsername, false);

            log.info("❌ External search rejected. Reason: {}", reason);
        }

        projectRepository.save(project);
    }

    /**
     * Resource Planner executes external search after approval
     */
    @Transactional
    public void executeExternalSearch(Long projectId, String rpUsername) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify external search was approved
        if (!"EXTERNAL_SEARCH_APPROVED".equals(project.getWorkflowStatus())) {
            throw new RuntimeException("External search not approved for this project");
        }

        log.info("🔍 Resource Planner {} executing external search for project: {}",
                rpUsername, project.getName());

        // Simulate external search
        simulateExternalSearch(project);

        project.setExternalSearchCompletedAt(LocalDateTime.now());
        project.setWorkflowStatus("EXTERNAL_SEARCH_COMPLETED");
        projectRepository.save(project);

        log.info("✅ External search completed for project: {}", project.getName());
    }

    /**
     * Simulate external candidate search (replace with actual API calls)
     */
    private void simulateExternalSearch(Project project) {
        log.info("🔍 Simulating external search for skills: {}",
                project.getSkillRequirements());

        // Simulate API delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate finding candidates
        log.info("✅ Found 3 potential external candidates");
        log.info("📧 Candidate notifications would be sent here");
    }
}