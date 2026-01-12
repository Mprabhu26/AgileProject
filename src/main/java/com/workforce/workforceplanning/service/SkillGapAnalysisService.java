// FILE: SkillGapAnalysisService.java
package com.workforce.workforceplanning.service;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.model.ProjectSkillRequirement;
import com.workforce.workforceplanning.model.Employee;
import com.workforce.workforceplanning.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SkillGapAnalysisService {

    private final EmployeeRepository employeeRepository;

    public SkillGapAnalysisService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * Calculate skill gaps for a project
     * Returns: {"Java": 2, "React": -1} where positive = available, negative = shortage
     */
    public Map<String, Integer> calculateSkillGaps(Project project) {
        Map<String, Integer> gaps = new HashMap<>();

        if (project.getSkillRequirements() == null || project.getSkillRequirements().isEmpty()) {
            return gaps; // No skill requirements = no gaps
        }

        // Get all available employees
        List<Employee> availableEmployees = employeeRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getAvailable()))  // ← CHANGE THIS LINE
                .collect(Collectors.toList());

        // For each required skill
        for (ProjectSkillRequirement requirement : project.getSkillRequirements()) {
            String skill = requirement.getSkill();
            int requiredCount = requirement.getRequiredCount();

            // Count how many available employees have this skill
            long employeeCount = availableEmployees.stream()
                    .filter(e -> e.getSkills().stream()
                            .anyMatch(empSkill -> empSkill.equalsIgnoreCase(skill)))
                    .count();

            // Calculate gap (positive = surplus, negative = shortage)
            int gap = (int) (employeeCount - requiredCount);
            gaps.put(skill, gap);
        }

        return gaps;
    }

    /**
     * Get critical skill gaps (shortages only)
     */
    public Map<String, Integer> getCriticalGaps(Project project) {
        Map<String, Integer> allGaps = calculateSkillGaps(project);

        return allGaps.entrySet().stream()
                .filter(entry -> entry.getValue() < 0) // Only shortages
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Math.abs(entry.getValue()) // Convert to positive number
                ));
    }

    /**
     * Get skill coverage percentage (0-100%)
     */
    public double getSkillCoveragePercentage(Project project) {
        Map<String, Integer> gaps = calculateSkillGaps(project);

        if (gaps.isEmpty()) {
            return 100.0; // No skill requirements = 100% covered
        }

        int totalRequired = project.getSkillRequirements().stream()
                .mapToInt(ProjectSkillRequirement::getRequiredCount)
                .sum();

        if (totalRequired == 0) return 100.0;

        int totalAvailable = gaps.values().stream()
                .mapToInt(gap -> Math.max(0, gap)) // Count only available (positive gaps)
                .sum();

        return (totalAvailable * 100.0) / totalRequired;
    }

    /**
     * Get recommended actions for skill gaps
     */
    public List<String> getRecommendedActions(Project project) {
        List<String> actions = new ArrayList<>();
        Map<String, Integer> criticalGaps = getCriticalGaps(project);

        if (criticalGaps.isEmpty()) {
            actions.add("✅ All skill requirements are met");
            return actions;
        }

        for (Map.Entry<String, Integer> entry : criticalGaps.entrySet()) {
            String skill = entry.getKey();
            int shortage = entry.getValue();

            if (shortage > 0) {
                actions.add("❌ Need " + shortage + " more employee(s) with " + skill + " skills");
            }
        }

        // Add general recommendations
        if (!criticalGaps.isEmpty()) {
            actions.add("💡 Consider: External hiring for missing skills");
            actions.add("💡 Consider: Upskilling existing employees");
            actions.add("💡 Consider: Adjusting project timeline");
        }

        return actions;
    }
}