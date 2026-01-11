// FILE: NotificationService.java
package com.workforce.workforceplanning.service;

import com.workforce.workforceplanning.model.Project;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import com.workforce.workforceplanning.model.Notification;
import com.workforce.workforceplanning.repository.NotificationRepository;



@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);


    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }


    public void sendSkillGapNotification(Project project, Map<String, Integer> criticalGaps) {
        String projectManagerUsername = project.getCreatedBy();

        log.info("🔔 SKILL GAP NOTIFICATION");
        log.info("To: Project Manager ({})", projectManagerUsername);
        log.info("Project: {} (ID: {})", project.getName(), project.getId());
        log.info("Critical Skill Gaps: {}", criticalGaps);
        log.info("Action Required: Please trigger external search for missing skills");



        // Just log for now
        logNotification(projectManagerUsername,
                "Skill Gap Alert for Project: " + project.getName(),
                "Missing skills: " + criticalGaps.keySet() + ". Please trigger external search.",
                "skill_gap",
                project.getId());
    }

    public void sendExternalSearchApprovalNotification(Long projectId, String projectName,
                                                       String approvedBy, boolean approved) {
        // Notify both Project Manager and Resource Planner
        log.info("🔔 EXTERNAL SEARCH {} NOTIFICATION", approved ? "APPROVED" : "REJECTED");
        log.info("Project: {} (ID: {})", projectName, projectId);
        log.info("Decision by: {}", approvedBy);
        log.info("Status: {}", approved ? "Approved" : "Rejected");

        // Determine which users to notify based on their roles
        // Project Manager: pm, pm2, pm3 (but need to know which one created the project)
        // Resource Planner: planner

        String message = approved ?
                "External search for project '" + projectName + "' has been APPROVED by " + approvedBy :
                "External search for project '" + projectName + "' has been REJECTED by " + approvedBy;

        log.info("Notifying Project Manager(s) and Resource Planner: {}", message);

        // Log notifications for different roles
        logNotification("PROJECT_MANAGER",
                "External Search " + (approved ? "Approved" : "Rejected"),
                message,
                "external_search_decision",
                projectId);

        logNotification("RESOURCE_PLANNER",
                "External Search " + (approved ? "Approved" : "Rejected"),
                message,
                "external_search_decision",
                projectId);
    }

    private void logNotification(String username, String title, String message,
                                 String type, Long projectId) {

        Notification notification = new Notification();
        notification.setUsername(username);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setProjectId(projectId);

        notificationRepository.save(notification);

        log.info("📝 Notification saved for user: {}", username);
    }


    /**
     * Get user email by username (simplified for your SecurityConfig users)
     */
    public String getUserEmail(String username) {
        // Map your in-memory users to emails
        switch (username) {
            case "pm":
                return "pm@example.com";
            case "pm2":
                return "pm2@example.com";
            case "pm3":
                return "pm3@example.com";
            case "head":
                return "head@example.com";
            case "planner":
                return "planner@example.com";
            case "employee1":
                return "employee1@example.com";
            default:
                return username + "@example.com";
        }
    }

    /**
     * Get user role(s) by username
     */
    public String getUserRole(String username) {
        // Map based on your SecurityConfig
        switch (username) {
            case "pm":
            case "pm2":
            case "pm3":
                return "PROJECT_MANAGER";
            case "head":
                return "DEPARTMENT_HEAD";
            case "planner":
                return "RESOURCE_PLANNER";
            case "employee1":
                return "EMPLOYEE";
            default:
                return "USER";
        }
    }
}