// FILE: SkillGapAnalysisDelegate.java - UPDATED VERSION
package com.workforce.workforceplanning.workflow;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.repository.ProjectRepository;
import com.workforce.workforceplanning.service.SkillGapAnalysisService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;

@Component
public class SkillGapAnalysisDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(SkillGapAnalysisDelegate.class);

    @Autowired
    private SkillGapAnalysisService skillGapAnalysisService;

    @Autowired
    private ProjectRepository projectRepository;

    @Override
    public void execute(DelegateExecution execution) {
        log.info("=== SKILL GAP ANALYSIS DELEGATE ===");

        try {
            // Get project ID from workflow variables
            Long projectId = (Long) execution.getVariable("projectId");

            if (projectId == null) {
                log.error("❌ projectId is null in workflow variables");
                execution.setVariable("hasCriticalSkillGaps", false);
                return;
            }

            log.info("Analyzing skill gaps for project ID: {}", projectId);

            // Get project from database
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found with ID: " + projectId));

            log.info("Project found: {} (ID: {})", project.getName(), project.getId());

            // Use the enhanced service to get detailed skill availability
            Map<String, Map<String, Object>> skillAvailabilityDetails =
                    skillGapAnalysisService.getSkillAvailabilityDetails(project);

            // Get formatted display text
            String formattedAvailability = skillGapAnalysisService.getSkillAvailabilityFormatted(project);

            // Get list format for UI
            List<Map<String, Object>> skillAvailabilityList =
                    skillGapAnalysisService.getSkillAvailabilityList(project);

            // Use existing method for critical gaps
            Map<String, Integer> criticalGaps = skillGapAnalysisService.getCriticalGaps(project);
            boolean hasCriticalSkillGaps = !criticalGaps.isEmpty();

            // Set workflow variables with new detailed information
            execution.setVariable("hasCriticalSkillGaps", hasCriticalSkillGaps);
            execution.setVariable("criticalGaps", criticalGaps);
            execution.setVariable("skillAvailabilityDetails", skillAvailabilityDetails);
            execution.setVariable("skillAvailabilityFormatted", formattedAvailability);
            execution.setVariable("skillAvailabilityList", skillAvailabilityList);
            execution.setVariable("skillCoveragePercentage",
                    skillGapAnalysisService.getSkillCoveragePercentage(project));

            // Log the detailed availability
            log.info("=== SKILL AVAILABILITY DETAILS ===");
            for (Map.Entry<String, Map<String, Object>> entry : skillAvailabilityDetails.entrySet()) {
                Map<String, Object> skillInfo = entry.getValue();
                log.info("{}: Required: {} | Available: {} | Has Gap: {} | Gap: {}",
                        skillInfo.get("skill"),
                        skillInfo.get("required"),
                        skillInfo.get("available"),
                        skillInfo.get("hasGap"),
                        skillInfo.get("gap"));
            }

            if (hasCriticalSkillGaps) {
                log.info("⚠️ Critical skill gaps detected for project: {}", project.getName());
                log.info("Missing skills: {}", criticalGaps);
                log.info("Skill coverage: {}%",
                        skillGapAnalysisService.getSkillCoveragePercentage(project));

                // Store missing skills as comma-separated string for notifications
                String missingSkills = String.join(", ", criticalGaps.keySet());
                execution.setVariable("missingSkills", missingSkills);

                // Get recommended actions
                execution.setVariable("recommendedActions",
                        skillGapAnalysisService.getRecommendedActions(project));
            } else {
                log.info("✅ No critical skill gaps detected for project: {}", project.getName());
                log.info("Skill coverage: {}%",
                        skillGapAnalysisService.getSkillCoveragePercentage(project));
            }

            log.info("✅ Skill gap analysis completed successfully");

        } catch (Exception e) {
            log.error("❌ Error in skill gap analysis", e);
            // Default to no gaps on error
            execution.setVariable("hasCriticalSkillGaps", false);
        }
    }
}