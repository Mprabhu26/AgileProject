package com.workforce.workforceplanning.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Delegate for sending rejection notifications
 * This is called automatically when the proposal is rejected
 */
@Component("rejectionNotificationDelegate")
public class RejectionNotificationDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(RejectionNotificationDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        logger.info("=== Sending Rejection Notification ===");

        // Get process variables
        Long projectId = (Long) execution.getVariable("projectId");
        String projectName = (String) execution.getVariable("projectName");
        String rejectionReason = (String) execution.getVariable("rejectionReason");
        String approver = (String) execution.getVariable("approver");

        logger.info("Project ID: {}", projectId);
        logger.info("Project Name: {}", projectName);
        logger.info("Rejected by: {}", approver);
        logger.info("Reason: {}", rejectionReason);

        // Here you could implement actual notification logic
        // For example: send email, create notification in database, etc.
        logger.info("Rejection notification sent to Resource Planner");

        // Set a variable to indicate notification was sent
        execution.setVariable("rejectionNotificationSent", true);
        execution.setVariable("notificationTimestamp", java.time.LocalDateTime.now().toString());

        logger.info("=== Rejection Notification Sent ===");
    }
}