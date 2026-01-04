package com.workforce.workforceplanning.workflow;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.model.ProjectStatus;
import com.workforce.workforceplanning.repository.ProjectRepository;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("projectApprovalDelegate")
public class ProjectApprovalDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ProjectApprovalDelegate.class);

    @Autowired
    private ProjectRepository projectRepository;

    @Override
    public void execute(DelegateExecution execution) {
        Object projectIdObj = execution.getVariable("projectId");
        Object approvedObj = execution.getVariable("approved");

        if (projectIdObj == null) {
            throw new RuntimeException("❌ projectId missing in process variables");
        }

        Long projectId;
        if (projectIdObj instanceof Number) {
            projectId = ((Number) projectIdObj).longValue();
        } else {
            projectId = Long.parseLong(projectIdObj.toString());
        }

        Boolean approved = approvedObj != null && Boolean.TRUE.equals(approvedObj);

        log.info("▶ Department approval received for projectId={}, approved={}", projectId, approved);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("❌ Project not found: " + projectId));

        if (approved) {
            project.setStatus(ProjectStatus.APPROVED);
            log.info("✅ Project {} approved", projectId);
        } else {
            project.setStatus(ProjectStatus.REJECTED);
            log.info("❌ Project {} rejected", projectId);
        }

        projectRepository.save(project);
    }
}