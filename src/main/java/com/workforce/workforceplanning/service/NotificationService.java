package com.workforce.workforceplanning.service;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.model.Notification;
import com.workforce.workforceplanning.model.NotificationType;
import com.workforce.workforceplanning.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Send skill gap notification to Project Manager
     */
    public void sendSkillGapNotification(Project project, Map<String, Integer> criticalGaps) {
        String projectManagerUsername = project.getCreatedBy();
        Long projectId = project.getId();

        log.info("🔔 SKILL GAP NOTIFICATION");
        log.info("To: Project Manager ({})", projectManagerUsername);
        log.info("Project: {} (ID: {})", project.getName(), projectId);
        log.info("Critical Skill Gaps: {}", criticalGaps);
        log.info("Action Required: Please trigger external search for missing skills");

        // Create notification for project manager
        // We need to convert PM username to employee ID - you'll need to add this logic
        Long pmEmployeeId = getEmployeeIdByUsername(projectManagerUsername);

        if (pmEmployeeId != null) {
            Notification notification = new Notification(
                    pmEmployeeId,
                    "Skill Gap Alert for Project: " + project.getName(),
                    "Missing skills: " + criticalGaps.keySet() + ". Please trigger external search.",
                    NotificationType.ASSIGNMENT_PROPOSED  // Use appropriate type
            );
            notification.setProjectId(projectId);
            notificationRepository.save(notification);

            log.info("📝 Skill gap notification saved for PM with employee ID: {}", pmEmployeeId);
        } else {
            log.warn("⚠️ Could not find employee ID for PM: {}", projectManagerUsername);
        }
    }

    /**
     * Send external search approval/rejection notification
     */
    public void sendExternalSearchApprovalNotification(Long projectId, String projectName,
                                                       String approvedBy, boolean approved) {
        log.info("🔔 EXTERNAL SEARCH {} NOTIFICATION", approved ? "APPROVED" : "REJECTED");
        log.info("Project: {} (ID: {})", projectName, projectId);
        log.info("Decision by: {}", approvedBy);
        log.info("Status: {}", approved ? "Approved" : "Rejected");

        String message = approved ?
                "External search for project '" + projectName + "' has been APPROVED by " + approvedBy :
                "External search for project '" + projectName + "' has been REJECTED by " + approvedBy;

        // Get employee IDs for PM and Resource Planner
        Long pmEmployeeId = getEmployeeIdByUsername("pm"); // You might want to get the actual PM
        Long plannerEmployeeId = getEmployeeIdByUsername("planner");

        // Notify Project Manager
        if (pmEmployeeId != null) {
            Notification pmNotification = new Notification(
                    pmEmployeeId,
                    "External Search " + (approved ? "Approved" : "Rejected"),
                    message,
                    NotificationType.ASSIGNMENT_PROPOSED  // Use appropriate type
            );
            pmNotification.setProjectId(projectId);
            notificationRepository.save(pmNotification);
            log.info("📝 External search notification saved for PM");
        }

        // Notify Resource Planner
        if (plannerEmployeeId != null) {
            Notification plannerNotification = new Notification(
                    plannerEmployeeId,
                    "External Search " + (approved ? "Approved" : "Rejected"),
                    message,
                    NotificationType.ASSIGNMENT_PROPOSED  // Use appropriate type
            );
            plannerNotification.setProjectId(projectId);
            notificationRepository.save(plannerNotification);
            log.info("📝 External search notification saved for Resource Planner");
        }
    }

    /**
     * Helper method to get employee ID by username
     * You'll need to implement this based on your Employee repository
     */
    private Long getEmployeeIdByUsername(String username) {
        // This is a placeholder - you need to implement this based on your data structure
        // You might need to inject EmployeeRepository or have another service

        // For now, return some default IDs based on username
        switch (username) {
            case "pm":
                return 1L;  // Example employee ID
            case "pm2":
                return 2L;
            case "pm3":
                return 3L;
            case "head":
                return 4L;
            case "planner":
                return 5L;
            case "employee1":
                return 6L;
            default:
                log.warn("⚠️ Unknown username for notification: {}", username);
                return null;
        }
    }

    /**
     * Get user email by username (simplified for your SecurityConfig users)
     */
    public String getUserEmail(String username) {
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