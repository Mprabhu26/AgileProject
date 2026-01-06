package com.workforce.workforceplanning.ui;

import com.workforce.workforceplanning.model.*;
import com.workforce.workforceplanning.repository.*;
import com.workforce.workforceplanning.service.AssignmentValidationService;
import com.workforce.workforceplanning.service.SkillGapAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ui/resource-planner")
public class ResourcePlannerUIController {

    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;
    private final AssignmentRepository assignmentRepository;
    private final ApplicationRepository applicationRepository;

    @Autowired
    private SkillGapAnalysisService skillGapAnalysisService;


    @Autowired
    private AssignmentValidationService validationService;

    public ResourcePlannerUIController(
            ProjectRepository projectRepository,
            EmployeeRepository employeeRepository,
            AssignmentRepository assignmentRepository,
            ApplicationRepository applicationRepository) {
        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
        this.assignmentRepository = assignmentRepository;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model,
                            @RequestParam(required = false) String view) {

        // Default view
        String activeView = (view != null) ? view : "projects";

        try {
            // Get published & approved projects
            List<Project> availableProjects = projectRepository.findAll().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getPublished()))
                    .filter(p -> p.getStatus() == ProjectStatus.APPROVED)
                    .collect(Collectors.toList());
            System.out.println("\n=== DEBUG: Dashboard - Available Projects ===");
            System.out.println("Total available projects: " + availableProjects.size());

            // Get available employees
            List<Employee> availableEmployees = employeeRepository.findAll().stream()
                    .filter(e -> Boolean.TRUE.equals(e.getAvailable()))
                    .collect(Collectors.toList());

            // Get all assignments
            List<Assignment> allAssignments = assignmentRepository.findAll();

            // Get pending applications count
            long pendingApplicationsCount = applicationRepository.findAll().stream()
                    .filter(app -> app.getStatus() == ApplicationStatus.PENDING)
                    .count();

            // Calculate staffing progress for each project
            Map<Long, StaffingInfo> projectStaffingInfo = new HashMap<>();
            for (Project project : availableProjects) {
                long assignedCount = allAssignments.stream()
                        .filter(a -> a.getProject().getId().equals(project.getId()))
                        .count();

                // Find matching available employees
                List<Employee> matchingEmployees = findMatchingEmployees(project);

                projectStaffingInfo.put(project.getId(),
                        new StaffingInfo((int) assignedCount, matchingEmployees.size()));
            }

            // Get skill distribution (for insights)
            if ("insights".equals(activeView)) {
                Map<String, Long> skillDistribution = employeeRepository.findAll().stream()
                        .flatMap(e -> e.getSkills().stream())
                        .collect(Collectors.groupingBy(skill -> skill, Collectors.counting()));

                Map<String, Long> departmentDistribution = employeeRepository.findAll().stream()
                        .collect(Collectors.groupingBy(Employee::getDepartment, Collectors.counting()));

                model.addAttribute("skillDistribution", skillDistribution);
                model.addAttribute("departmentDistribution", departmentDistribution);
            }

            // Add to model
            model.addAttribute("availableProjects", availableProjects);
            model.addAttribute("availableEmployees", availableEmployees);
            model.addAttribute("assignments", allAssignments);
            model.addAttribute("projectStaffingInfo", projectStaffingInfo);
            model.addAttribute("pendingApplicationsCount", pendingApplicationsCount);
            model.addAttribute("activeView", activeView);

            // For employee search view
            if ("employees".equals(activeView)) {
                Set<String> allSkills = employeeRepository.findAll().stream()
                        .flatMap(e -> e.getSkills().stream())
                        .collect(Collectors.toSet());

                Set<String> allDepartments = employeeRepository.findAll().stream()
                        .map(Employee::getDepartment)
                        .collect(Collectors.toSet());

                model.addAttribute("allSkills", allSkills);
                model.addAttribute("allDepartments", allDepartments);
            }

            return "resource-planner/dashboard";

        } catch (Exception e) {
            // If there's an error, return empty lists
            model.addAttribute("availableProjects", List.of());
            model.addAttribute("availableEmployees", List.of());
            model.addAttribute("activeView", activeView);
            return "resource-planner/dashboard";
        }
    }

    @GetMapping("/project/{projectId}")
    public String viewProjectStaffing(@PathVariable Long projectId, Model model) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Get current assignments for this project
        List<Assignment> currentAssignments = assignmentRepository.findAll().stream()
                .filter(a -> a.getProject().getId().equals(projectId))
                .collect(Collectors.toList());

        // Find matching employees
        List<Employee> matchingEmployees = findMatchingEmployees(project);

        // Get pending applications for this project
        List<Application> pendingApplications = applicationRepository.findAll().stream()
                .filter(app -> app.getProject().getId().equals(projectId))
                .filter(app -> app.getStatus() == ApplicationStatus.PENDING)
                .collect(Collectors.toList());

        // Get all skills for filter
        Set<String> allSkills = employeeRepository.findAll().stream()
                .flatMap(e -> e.getSkills().stream())
                .collect(Collectors.toSet());

        // Skill Gap Analysis
        Map<String, Integer> skillGaps = skillGapAnalysisService.calculateSkillGaps(project);
        double skillCoveragePercentage = skillGapAnalysisService.getSkillCoveragePercentage(project);
        List<String> recommendedActions = skillGapAnalysisService.getRecommendedActions(project);

        model.addAttribute("skillGaps", skillGaps);
        model.addAttribute("skillCoveragePercentage", skillCoveragePercentage);
        model.addAttribute("recommendedActions", recommendedActions);

        // Helper methods for template - FIXED VERSION
        Map<String, Object> templateHelpers = new HashMap<>();
        templateHelpers.put("getRequiredCount", new BiFunction<Project, String, Integer>() {
            @Override
            public Integer apply(Project p, String skill) {
                return p.getSkillRequirements().stream()
                        .filter(req -> req.getSkill().equals(skill))
                        .findFirst()
                        .map(ProjectSkillRequirement::getRequiredCount)
                        .orElse(0);
            }
        });

        // Create a simple utility class for the triple function
        templateHelpers.put("getAvailableCount", new TriFunction<Project, String, List<Employee>, Integer>() {
            @Override
            public Integer apply(Project p, String skill, List<Employee> availableEmployees) {
                return (int) availableEmployees.stream()
                        .filter(e -> e.hasSkill(skill))
                        .count();
            }
        });

        model.addAttribute("helpers", templateHelpers);
        model.addAttribute("availableEmployeesForGap", matchingEmployees); // Pass matching employees for gap calculation

        model.addAttribute("project", project);
        model.addAttribute("currentAssignments", currentAssignments);
        model.addAttribute("matchingEmployees", matchingEmployees);
        model.addAttribute("pendingApplications", pendingApplications);
        model.addAttribute("allSkills", allSkills);

        return "resource-planner/project-staffing";
    }

    @PostMapping("/project/{projectId}/propose")
    public String proposeEmployee(
            @PathVariable Long projectId,
            @RequestParam Long employeeId,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // USE THE VALIDATION SERVICE (replaces the old duplicate check)
        AssignmentValidationService.ValidationResult validation = validationService.canAssignEmployeeToProject(projectId, employeeId);

        if (validation.isError()) {
            redirectAttributes.addFlashAttribute("error",
                    employee.getName() + " - " + validation.getMessage());
            return "redirect:/ui/resource-planner/project/" + projectId;
        }

        // Create PENDING assignment (shows in UI immediately)
        Assignment pendingAssignment = new Assignment(project, employee, AssignmentStatus.PENDING);
        pendingAssignment.setAssignedAt(java.time.LocalDateTime.now());
        assignmentRepository.save(pendingAssignment);

        // Employee stays unavailable until reject
        employee.setAvailable(false);
        employeeRepository.save(employee);

        // Log for workflow (your existing code)
        System.out.println("Resource Planner proposing employee " + employee.getName() +
                " for project " + project.getName() +
                (notes != null ? ". Notes: " + notes : ""));

        redirectAttributes.addFlashAttribute("success",
                "Proposed " + employee.getName() + " for " + project.getName() +
                        ". Awaiting Department Head approval.");

        return "redirect:/ui/resource-planner/project/" + projectId;
    }

    @GetMapping("/search")
    public String searchEmployees(
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Boolean available,
            Model model) {

        List<Employee> employees = employeeRepository.findAll();

        // Apply filters
        if (skills != null && !skills.trim().isEmpty()) {
            String[] skillArray = skills.toLowerCase().split(",");
            employees = employees.stream()
                    .filter(e -> e.getSkills().stream()
                            .anyMatch(skill -> Arrays.stream(skillArray)
                                    .anyMatch(searchSkill ->
                                            skill.toLowerCase().contains(searchSkill.trim()))))
                    .collect(Collectors.toList());
        }

        if (department != null && !department.trim().isEmpty()) {
            employees = employees.stream()
                    .filter(e -> e.getDepartment().toLowerCase().contains(department.toLowerCase().trim()))
                    .collect(Collectors.toList());
        }

        if (available != null) {
            employees = employees.stream()
                    .filter(e -> available.equals(e.getAvailable()))
                    .collect(Collectors.toList());
        }

        // Get all unique filters for dropdowns
        Set<String> allSkills = employeeRepository.findAll().stream()
                .flatMap(e -> e.getSkills().stream())
                .collect(Collectors.toSet());

        Set<String> allDepartments = employeeRepository.findAll().stream()
                .map(Employee::getDepartment)
                .collect(Collectors.toSet());

        model.addAttribute("employees", employees);
        model.addAttribute("allSkills", allSkills);
        model.addAttribute("allDepartments", allDepartments);
        model.addAttribute("searchSkills", skills);
        model.addAttribute("searchDepartment", department);
        model.addAttribute("searchAvailable", available);

        return "resource-planner/employee-search";
    }

    @PostMapping("/assignment/{assignmentId}/remove")
    public String removeAssignment(
            @PathVariable Long assignmentId,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttributes) {

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        Long projectId = assignment.getProject().getId();

        // Remove assignment and free up employee
        Employee employee = assignment.getEmployee();
        employee.setAvailable(true);
        employeeRepository.save(employee);

        assignmentRepository.deleteById(assignmentId);

        redirectAttributes.addFlashAttribute("success",
                "Removed " + employee.getName() + " from project" +
                        (reason != null ? ". Reason: " + reason : ""));

        return "redirect:/ui/resource-planner/project/" + projectId;
    }

    // Helper method to find employees matching project requirements
    // Helper method to find employees matching project requirements (CASE-INSENSITIVE)
    private List<Employee> findMatchingEmployees(Project project) {
        if (project.getSkillRequirements() == null || project.getSkillRequirements().isEmpty()) {
            // If no specific skills required, return all available employees
            return employeeRepository.findAll().stream()
                    .filter(e -> Boolean.TRUE.equals(e.getAvailable()))
                    .collect(Collectors.toList());
        }

        // Get required skills from project (convert to lowercase for case-insensitive matching)
        Set<String> requiredSkills = project.getSkillRequirements().stream()
                .map(req -> req.getSkill().toLowerCase().trim())
                .collect(Collectors.toSet());

        // DEBUG: Print what we're looking for
        System.out.println("Looking for skills (case-insensitive): " + requiredSkills);

        // Find available employees with matching skills (CASE-INSENSITIVE)
        return employeeRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getAvailable()))
                .filter(e -> {
                    if (e.getSkills() == null || e.getSkills().isEmpty()) {
                        return false;
                    }

                    // Convert employee skills to lowercase for comparison
                    Set<String> employeeSkills = e.getSkills().stream()
                            .map(skill -> skill.toLowerCase().trim())
                            .collect(Collectors.toSet());

                    // Check if any required skill matches any employee skill
                    return requiredSkills.stream()
                            .anyMatch(requiredSkill ->
                                    employeeSkills.stream()
                                            .anyMatch(employeeSkill ->
                                                    employeeSkill.equals(requiredSkill)));
                })
                .sorted((e1, e2) -> {
                    // Sort by number of matching skills (descending)
                    // Convert to lowercase for comparison
                    Set<String> e1Skills = e1.getSkills().stream()
                            .map(skill -> skill.toLowerCase().trim())
                            .collect(Collectors.toSet());
                    Set<String> e2Skills = e2.getSkills().stream()
                            .map(skill -> skill.toLowerCase().trim())
                            .collect(Collectors.toSet());

                    long e1Matches = requiredSkills.stream()
                            .filter(e1Skills::contains).count();
                    long e2Matches = requiredSkills.stream()
                            .filter(e2Skills::contains).count();
                    return Long.compare(e2Matches, e1Matches);
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/assignment/{assignmentId}/cancel")
    @ResponseBody
    public String cancelProposal(@PathVariable Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        // Only cancel if still PENDING
        if(assignment.getStatus() == AssignmentStatus.PENDING) {
            //Make employee available again
            Employee employee = assignment.getEmployee();
            employee.setAvailable(true);
            employeeRepository.save(employee);

            assignmentRepository.delete(assignment);
            return "Proposal cancelled - employee is now available";
        }

        return "Cannot cancel - already processed";
    }

    @PostMapping("/project/{id}/external-search")
    public String triggerExternalSearch(
            @PathVariable Long id,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Mark project for external search
        project.setExternalSearchNeeded(true);
        project.setExternalSearchNotes(notes);
        project.setExternalSearchRequestedAt(java.time.LocalDateTime.now());
        projectRepository.save(project);

        // TODO: In real implementation, integrate with external APIs here

        redirectAttributes.addFlashAttribute("success",
                "External search initiated for project: " + project.getName() +
                        ". Recruitment team has been notified.");

        return "redirect:/ui/resource-planner/project/" + id;
    }

    // Add this method to view external candidates (placeholder)
    @GetMapping("/project/{id}/external-candidates")
    public String viewExternalCandidates(@PathVariable Long id, Model model) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        model.addAttribute("project", project);

        // TODO: In real implementation, fetch from external APIs
        List<Map<String, String>> externalCandidates = new ArrayList<>();

        // Placeholder data
        // Placeholder data with location
        externalCandidates.add(Map.of(
                "name", "External Candidate 1",
                "skills", "Java, Spring Boot, AWS",
                "source", "LinkedIn",
                "experience", "5 years",
                "location", "San Francisco, CA"  // Add location
        ));

        externalCandidates.add(Map.of(
                "name", "External Candidate 2",
                "skills", "React, Node.js, MongoDB",
                "source", "Indeed",
                "experience", "3 years",
                "location", "Remote"  // Add location
        ));

        model.addAttribute("externalCandidates", externalCandidates);
        model.addAttribute("hasExternalCandidates", !externalCandidates.isEmpty());

        return "resource-planner/external-candidates";
    }

    // Helper class for staffing information
    private static class StaffingInfo {
        int assignedCount;
        int matchingEmployeesCount;

        StaffingInfo(int assignedCount, int matchingEmployeesCount) {
            this.assignedCount = assignedCount;
            this.matchingEmployeesCount = matchingEmployeesCount;
        }

        public int getAssignedCount() { return assignedCount; }
        public int getMatchingEmployeesCount() { return matchingEmployeesCount; }
    }

    // Custom functional interface for 3 parameters
    @FunctionalInterface
    interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}