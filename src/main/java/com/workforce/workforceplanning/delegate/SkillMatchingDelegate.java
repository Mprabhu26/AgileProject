package com.workforce.workforceplanning.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Delegate for automatic skill matching service task
 * This is called automatically when the BPMN process reaches "Auto Skill Matching" task
 */
@Component("skillMatchingDelegate")
public class SkillMatchingDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(SkillMatchingDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        logger.info("=== Auto Skill Matching Started ===");

        // Get process variables
        Long projectId = (Long) execution.getVariable("projectId");
        String projectName = (String) execution.getVariable("projectName");

        logger.info("Project ID: {}", projectId);
        logger.info("Project Name: {}", projectName);

        // Here you could implement actual skill matching logic
        // For now, we'll just log that it happened
        logger.info("Skill matching completed for project: {}", projectName);

        // Set a variable to indicate matching is done
        execution.setVariable("skillMatchingCompleted", true);
        execution.setVariable("matchingTimestamp", java.time.LocalDateTime.now().toString());

        logger.info("=== Auto Skill Matching Completed ===");
    }
}