// FILE: NotificationDelegate.java (update it)
package com.workforce.workforceplanning.workflow;

import com.workforce.workforceplanning.service.NotificationService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("notificationDelegate")
public class NotificationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(NotificationDelegate.class);

    @Autowired
    private NotificationService notificationService;

    @Override
    public void execute(DelegateExecution execution) {
        log.info("=== NOTIFICATION DELEGATE EXECUTED ===");

        // Check what type of notification this is
        String notificationType = (String) execution.getVariable("notificationType");

        if (notificationType == null) {
            // Default to assignment notification (your existing code)
            sendAssignmentNotification(execution);
        } else {
            // Handle different notification types
            switch (notificationType) {
                case "skill_gap":
                    sendSkillGapNotification(execution);
                    break;
                case "external_search_approved":
                    sendExternalSearchApprovedNotification(execution);
                    break;
                case "external_search_rejected":
                    sendExternalSearchRejectedNotification(execution);
                    break;
                default:
                    sendAssignmentNotification(execution);
            }
        }

        log.info("✅ Notification sent successfully");
    }

    private void sendAssignmentNotification(DelegateExecution execution) {
        // Your existing assignment notification code
        Object employeeEmailObj = execution.getVariable("employeeEmail");
        Object employeeNameObj = execution.getVariable("employeeName");
        Object projectNameObj = execution.getVariable("projectName");
        Object projectIdObj = execution.getVariable("projectId");

        String employeeEmail = employeeEmailObj != null ? employeeEmailObj.toString() : "unknown";
        String employeeName = employeeNameObj != null ? employeeNameObj.toString() : "unknown";
        String projectName = projectNameObj != null ? projectNameObj.toString() : "unknown";

        Long projectId = 0L;
        if (projectIdObj instanceof Number) {
            projectId = ((Number) projectIdObj).longValue();
        } else if (projectIdObj != null) {
            projectId = Long.parseLong(projectIdObj.toString());
        }

        log.info("📧 Sending assignment notification to: {} <{}>", employeeName, employeeEmail);
        log.info("📋 About project: {} (ID: {})", projectName, projectId);
    }

    private void sendSkillGapNotification(DelegateExecution execution) {
        Long projectId = (Long) execution.getVariable("projectId");
        String projectName = (String) execution.getVariable("projectName");
        String missingSkills = (String) execution.getVariable("missingSkills");

        log.info("🔔 Sending skill gap notification");
        log.info("Project: {} (ID: {})", projectName, projectId);
        log.info("Missing Skills: {}", missingSkills);

        // You could call notificationService here if needed
        // For now, just log it
    }

    private void sendExternalSearchApprovedNotification(DelegateExecution execution) {
        Long projectId = (Long) execution.getVariable("projectId");
        String projectName = (String) execution.getVariable("projectName");
        String approvedBy = (String) execution.getVariable("approvedBy");

        log.info("✅ Sending external search approved notification");
        log.info("Project: {} (ID: {})", projectName, projectId);
        log.info("Approved by: {}", approvedBy);

        // Use your NotificationService
        notificationService.sendExternalSearchApprovalNotification(projectId, projectName, approvedBy, true);
    }

    private void sendExternalSearchRejectedNotification(DelegateExecution execution) {
        Long projectId = (Long) execution.getVariable("projectId");
        String projectName = (String) execution.getVariable("projectName");
        String rejectedBy = (String) execution.getVariable("rejectedBy");

        log.info("❌ Sending external search rejected notification");
        log.info("Project: {} (ID: {})", projectName, projectId);
        log.info("Rejected by: {}", rejectedBy);

        // Use your NotificationService
        notificationService.sendExternalSearchApprovalNotification(projectId, projectName, rejectedBy, false);
    }
}