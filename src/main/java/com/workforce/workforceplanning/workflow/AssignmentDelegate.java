package com.workforce.workforceplanning.workflow;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("assignmentDelegate")
public class AssignmentDelegate implements JavaDelegate { // NO @Component

    private static final Logger log = LoggerFactory.getLogger(AssignmentDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        log.info("=== ASSIGNMENT DELEGATE EXECUTED ===");

        try {
            Object projectIdObj = execution.getVariable("projectId");
            Object employeeIdsObj = execution.getVariable("employeeIds");

            log.info("Assignment for project: {}", projectIdObj);
            log.info("Employees to assign: {}", employeeIdsObj);

            // Just log - database updates will be handled elsewhere
            log.info("✅ Assignment delegate executed successfully");

        } catch (Exception e) {
            log.error("❌ Error in assignment delegate", e);
            throw e;
        }
    }
}