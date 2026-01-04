package com.workforce.workforceplanning.workflow;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("notificationDelegate")
public class NotificationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(NotificationDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        log.info("🔥 NotificationDelegate EXECUTED");

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

        log.info("📧 Sending notification to: {} <{}>", employeeName, employeeEmail);
        log.info("📋 About project: {} (ID: {})", projectName, projectId);

        log.info("✅ Notification sent successfully");
    }
}