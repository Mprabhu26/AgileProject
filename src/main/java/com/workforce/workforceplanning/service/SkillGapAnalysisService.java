// FILE: SkillGapAnalysisService.java - ENHANCED VERSION
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
                .filter(e -> Boolean.TRUE.equals(e.getAvailable()))
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
     * NEW: Get skill availability in "Required: X | Available: Y" format
     * Returns a map with detailed information for each skill
     */
    public Map<String, Map<String, Object>> getSkillAvailabilityDetails(Project project) {
        Map<String, Map<String, Object>> skillDetails = new LinkedHashMap<>();

        if (project.getSkillRequirements() == null || project.getSkillRequirements().isEmpty()) {
            return skillDetails;
        }

        // Get all available employees
        List<Employee> availableEmployees = employeeRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getAvailable()))
                .collect(Collectors.toList());

        // For each required skill
        for (ProjectSkillRequirement requirement : project.getSkillRequirements()) {
            String skill = requirement.getSkill();
            int requiredCount = requirement.getRequiredCount();

            // Find employees with this skill
            List<Employee> employeesWithSkill = availableEmployees.stream()
                    .filter(e -> e.getSkills().stream()
                            .anyMatch(empSkill -> empSkill.equalsIgnoreCase(skill)))
                    .collect(Collectors.toList());

            int availableCount = employeesWithSkill.size();
            boolean isAvailable = availableCount >= requiredCount;

            // Calculate gap
            int gap = requiredCount - availableCount;
            boolean hasGap = gap > 0;

            // Create detailed info map
            Map<String, Object> skillInfo = new HashMap<>();
            skillInfo.put("skill", skill);
            skillInfo.put("required", requiredCount);
            skillInfo.put("available", availableCount);
            skillInfo.put("isAvailable", isAvailable);
            skillInfo.put("hasGap", hasGap);
            skillInfo.put("gap", gap);
            skillInfo.put("employees", employeesWithSkill.stream()
                    .map(Employee::getName)
                    .collect(Collectors.toList()));

            skillDetails.put(skill, skillInfo);
        }

        return skillDetails;
    }

    /**
     * NEW: Get formatted string for UI display "Required: X | Available: Y"
     */
    public String getSkillAvailabilityFormatted(Project project) {
        if (project.getSkillRequirements() == null || project.getSkillRequirements().isEmpty()) {
            return "No specific skill requirements";
        }

        Map<String, Map<String, Object>> details = getSkillAvailabilityDetails(project);
        List<String> formatted = new ArrayList<>();

        for (Map<String, Object> skillInfo : details.values()) {
            String skill = (String) skillInfo.get("skill");
            int required = (int) skillInfo.get("required");
            int available = (int) skillInfo.get("available");
            boolean hasGap = (boolean) skillInfo.get("hasGap");

            String status = hasGap ? "❌ " : "✅ ";
            formatted.add(status + skill + ": Required: " + required + " | Available: " + available);
        }

        return String.join("\n", formatted);
    }

    /**
     * NEW: Get skill availability summary for display in tables/cards
     */
    public List<Map<String, Object>> getSkillAvailabilityList(Project project) {
        List<Map<String, Object>> skillList = new ArrayList<>();

        if (project.getSkillRequirements() == null || project.getSkillRequirements().isEmpty()) {
            return skillList;
        }

        Map<String, Map<String, Object>> details = getSkillAvailabilityDetails(project);

        for (Map.Entry<String, Map<String, Object>> entry : details.entrySet()) {
            Map<String, Object> skillInfo = new HashMap<>(entry.getValue());

            // Add display properties
            skillInfo.put("displayText",
                    "Required: " + skillInfo.get("required") +
                            " | Available: " + skillInfo.get("available"));

            skillList.add(skillInfo);
        }

        return skillList;
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
                .mapToInt(gap -> {
                    // For each skill, count only up to required count
                    // Positive gaps mean we have more than needed, so count only what's needed
                    // Negative gaps mean shortage, so count 0
                    return gap >= 0 ? 0 : Math.abs(gap);
                })
                .sum();

        int totalCovered = totalRequired - totalAvailable;

        return (totalCovered * 100.0) / totalRequired;
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

        // Get detailed availability for better recommendations
        Map<String, Map<String, Object>> details = getSkillAvailabilityDetails(project);

        for (Map.Entry<String, Integer> entry : criticalGaps.entrySet()) {
            String skill = entry.getKey();
            int shortage = entry.getValue();

            // Get available count for this skill
            Map<String, Object> skillInfo = details.get(skill);
            int available = skillInfo != null ? (int) skillInfo.get("available") : 0;
            int required = skillInfo != null ? (int) skillInfo.get("required") : 0;

            actions.add("❌ " + skill + ": Required: " + required + " | Available: " + available +
                    " (Need " + shortage + " more)");
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