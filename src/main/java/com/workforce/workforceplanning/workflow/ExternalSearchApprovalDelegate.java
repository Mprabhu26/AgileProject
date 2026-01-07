// FILE: ExternalSearchApprovalDelegate.java
package com.workforce.workforceplanning.workflow;

import com.workforce.workforceplanning.service.ExternalSearchService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("externalSearchApprovalDelegate")
public class ExternalSearchApprovalDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ExternalSearchApprovalDelegate.class);

    @Autowired
    private ExternalSearchService externalSearchService;

    @Override
    public void execute(DelegateExecution execution) {
        log.info("=== PROCESSING EXTERNAL SEARCH DECISION ===");

        try {
            // Get variables
            Long projectId = (Long) execution.getVariable("projectId");
            Boolean approved = (Boolean) execution.getVariable("approved");
            String approver = (String) execution.getVariable("approvedBy");
            String reason = approved ?
                    (String) execution.getVariable("approvalNotes") :
                    (String) execution.getVariable("rejectionReason");

            if (approver == null) {
                approver = "DepartmentHead";
            }

            if (reason == null) {
                reason = approved ? "Approved by Department Head" : "Rejected by Department Head";
            }

            log.info("Project ID: {}", projectId);
            log.info("Decision: {}", approved ? "APPROVED" : "REJECTED");
            log.info("Approver: {}", approver);
            log.info("Reason: {}", reason);

            // Process the decision
            externalSearchService.processExternalSearchDecision(projectId, approver, approved, reason);

            log.info("✅ External search decision processed successfully");

        } catch (Exception e) {
            log.error("❌ Error processing external search decision", e);
            throw e;
        }
    }
}