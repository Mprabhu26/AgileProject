// FILE: ExternalSearchDecisionDelegate.java
package com.workforce.workforceplanning.workflow;

import com.workforce.workforceplanning.service.ExternalSearchService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExternalSearchDecisionDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ExternalSearchDecisionDelegate.class);

    @Autowired
    private ExternalSearchService externalSearchService;

    @Override
    public void execute(DelegateExecution execution) {
        log.info("=== PROCESSING EXTERNAL SEARCH DECISION ===");

        try {
            Long projectId = (Long) execution.getVariable("projectId");
            Boolean externalSearchApproved = (Boolean) execution.getVariable("externalSearchApproved");
            String approver = (String) execution.getVariable("approvedBy");
            String notes = (String) execution.getVariable("externalSearchNotes");

            if (approver == null) {
                approver = "DepartmentHead";
            }

            if (notes == null) {
                notes = externalSearchApproved ?
                        "External search approved by Department Head" :
                        "External search rejected by Department Head";
            }

            log.info("Processing external search decision for project: {}", projectId);
            log.info("Decision: {} by {}", externalSearchApproved ? "APPROVED" : "REJECTED", approver);
            log.info("Notes: {}", notes);

            // Store decision in variables for notifications
            execution.setVariable("externalSearchDecisionProcessed", true);
            execution.setVariable("decisionTimestamp", new java.util.Date());

        } catch (Exception e) {
            log.error("❌ Error processing external search decision", e);
            // Set to false on error
            execution.setVariable("externalSearchApproved", false);
        }
    }
}