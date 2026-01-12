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
import com.workforce.workforceplanning.service.ExternalSearchService;
import com.workforce.workforceplanning.service.NotificationService;
import com.workforce.workforceplanning.service.SkillGapAnalysisService;
import org.slf4j.Logger; // Add this import
import org.slf4j.LoggerFactory;
import java.security.Principal;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ui/resource-planner")

public class ResourcePlannerUIController {
    private static final Logger log = LoggerFactory.getLogger(ResourcePlannerUIController.class);
    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;
    private final AssignmentRepository assignmentRepository;
    private final ApplicationRepository applicationRepository;
    private final ExternalSearchService externalSearchService;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @Autowired
    private SkillGapAnalysisService skillGapAnalysisService;


    @Autowired
    private AssignmentValidationService validationService;

    public ResourcePlannerUIController(
            ProjectRepository projectRepository,
            EmployeeRepository employeeRepository,
            AssignmentRepository assignmentRepository,
            ApplicationRepository applicationRepository,
            ExternalSearchService externalSearchService,
            NotificationService notificationService,
            NotificationRepository notificationRepository,
            SkillGapAnalysisService skillGapAnalysisService) {
        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
        this.assignmentRepository = assignmentRepository;
        this.applicationRepository = applicationRepository;
        this.externalSearchService = externalSearchService;      // INITIALIZE
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.skillGapAnalysisService = skillGapAnalysisService;
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

        // Find matching employees (available employees with matching skills)
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

        // ----- SKILL COVERAGE CALCULATION (Based on assigned employees) -----

        // Get employees currently assigned to this project
        List<Employee> assignedEmployees = currentAssignments.stream()
                .map(Assignment::getEmployee)
                .collect(Collectors.toList());

        // Calculate skill coverage based on ASSIGNED employees
        Map<String, Integer> assignedSkillCounts = new HashMap<>();
        for (Employee emp : assignedEmployees) {
            if (emp.getSkills() != null) {
                for (String skill : emp.getSkills()) {
                    String skillLower = skill.toLowerCase().trim();
                    assignedSkillCounts.merge(skillLower, 1, Integer::sum);
                }
            }
        }

        // Calculate coverage percentage
        int totalSkillsNeeded = 0;
        int skillsCovered = 0;

        if (project.getSkillRequirements() != null) {
            for (ProjectSkillRequirement req : project.getSkillRequirements()) {
                String reqSkill = req.getSkill().toLowerCase().trim();
                int required = req.getRequiredCount();
                int available = assignedSkillCounts.getOrDefault(reqSkill, 0);

                totalSkillsNeeded += required;
                skillsCovered += Math.min(available, required);
            }
        }

        // Calculate coverage based on assigned employees
        double calculatedCoveragePercentage = 0;
        if (totalSkillsNeeded > 0) {
            calculatedCoveragePercentage = ((double) skillsCovered / totalSkillsNeeded) * 100;
        }

        // ----- SKILL GAP CALCULATION (Based on assigned employees) -----

        Map<String, Integer> skillGaps = new HashMap<>();
        if (project.getSkillRequirements() != null) {
            for (ProjectSkillRequirement req : project.getSkillRequirements()) {
                String reqSkill = req.getSkill().toLowerCase().trim();
                int required = req.getRequiredCount();

                // Count from assigned employees
                int assignedCount = assignedSkillCounts.getOrDefault(reqSkill, 0);

                // Calculate gap (positive = shortage)
                int gap = required - assignedCount;
                if (gap > 0) {
                    skillGaps.put(req.getSkill(), gap); // Use original case for display
                }
            }
        }

        boolean hasSkillGaps = !skillGaps.isEmpty();

        // Calculate missing employees count correctly
        int missingEmployeesCount = skillGaps.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        // Calculate covered skills count (skills that are fully met)
        int coveredSkillsCount = 0;
        if (project.getSkillRequirements() != null) {
            for (ProjectSkillRequirement req : project.getSkillRequirements()) {
                String reqSkill = req.getSkill().toLowerCase().trim();
                int assignedCount = assignedSkillCounts.getOrDefault(reqSkill, 0);
                if (assignedCount >= req.getRequiredCount()) {
                    coveredSkillsCount++;
                }
            }
        }

        // ----- EXTERNAL SEARCH & PM NOTIFICATION -----

        // Check if PM already notified
        boolean pmNotified = Boolean.TRUE.equals(project.getExternalSearchNeeded()) &&
                "AWAITING_PM_DECISION".equals(project.getWorkflowStatus());

        // ----- AVAILABLE EMPLOYEES FOR GAP ANALYSIS -----

        // Get available employees (for display in gap analysis)
        List<Employee> availableEmployeesForGap = employeeRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getAvailable()))
                .collect(Collectors.toList());

        // ----- SKILL REQUIREMENT COUNTS -----

        // Pre-calculate required counts for each skill
        Map<String, Integer> requiredCounts = new HashMap<>();
        if (project.getSkillRequirements() != null) {
            for (ProjectSkillRequirement req : project.getSkillRequirements()) {
                requiredCounts.put(req.getSkill(), req.getRequiredCount());
            }
        }

        // Pre-calculate available counts for each skill (from ALL available employees)
        Map<String, Integer> availableCounts = new HashMap<>();
        if (availableEmployeesForGap != null) {
            for (Employee emp : availableEmployeesForGap) {
                if (emp.getSkills() != null) {
                    for (String skill : emp.getSkills()) {
                        String skillLower = skill.toLowerCase().trim();
                        availableCounts.merge(skillLower, 1, Integer::sum);
                    }
                }
            }
        }

        // Create display version with original case
        Map<String, Integer> displayAvailableCounts = new HashMap<>();
        if (project.getSkillRequirements() != null) {
            for (ProjectSkillRequirement req : project.getSkillRequirements()) {
                String reqSkill = req.getSkill().toLowerCase().trim();

                // Find matching available skills (case-insensitive)
                int count = 0;
                for (Map.Entry<String, Integer> availEntry : availableCounts.entrySet()) {
                    if (availEntry.getKey().equals(reqSkill)) {
                        count += availEntry.getValue();
                    }
                }
                displayAvailableCounts.put(req.getSkill(), count); // Keep original case for display
            }
        }

        // ----- RECOMMENDED ACTIONS -----

        List<String> recommendedActions = new ArrayList<>();
        if (!skillGaps.isEmpty()) {
            for (Map.Entry<String, Integer> gap : skillGaps.entrySet()) {
                if (gap.getValue() > 0) { // Shortage
                    recommendedActions.add("Find " + gap.getValue() +
                            " more employee(s) with " + gap.getKey() + " skills");
                }
            }
            if (!recommendedActions.isEmpty()) {
                recommendedActions.add("Consider external hiring for critical skills");
                recommendedActions.add("Check if employees can be cross-trained");
            }
        }

        boolean hasCoveredSkills = false;
        if (skillGaps != null && requiredCounts != null) {
            for (String skill : requiredCounts.keySet()) {
                Integer gap = skillGaps.get(skill);
                if (gap == null || gap <= 0) {
                    hasCoveredSkills = true;
                    break;
                }
            }
        }

        // ----- SERVICE-BASED ANALYSIS (KEEP FOR BACKWARD COMPATIBILITY) -----

        // Call service for skill gap analysis (might be used elsewhere)
        Map<String, Integer> serviceSkillGaps = skillGapAnalysisService.getCriticalGaps(project);
        double serviceSkillCoverage = skillGapAnalysisService.getSkillCoveragePercentage(project);

        // Add everything to model
        model.addAttribute("project", project);
        model.addAttribute("currentAssignments", currentAssignments);
        model.addAttribute("matchingEmployees", matchingEmployees);
        model.addAttribute("pendingApplications", pendingApplications);
        model.addAttribute("allSkills", allSkills);
        model.addAttribute("skillGaps", skillGaps); // Our calculated gaps
        model.addAttribute("serviceSkillGaps", serviceSkillGaps); // Service gaps
        model.addAttribute("hasSkillGaps", hasSkillGaps);
        model.addAttribute("skillCoverage", serviceSkillCoverage); // Service coverage
        model.addAttribute("skillCoveragePercentage", calculatedCoveragePercentage); // Our coverage
        model.addAttribute("pmNotified", pmNotified);
        model.addAttribute("availableEmployeesForGap", availableEmployeesForGap);
        model.addAttribute("requiredCounts", requiredCounts);
        model.addAttribute("availableCounts", displayAvailableCounts); // For display
        model.addAttribute("recommendedActions", recommendedActions);
        model.addAttribute("missingEmployeesCount", missingEmployeesCount);
        model.addAttribute("coveredSkillsCount", coveredSkillsCount);
        model.addAttribute("totalSkillsNeeded", totalSkillsNeeded);
        model.addAttribute("skillsCovered", skillsCovered);
        model.addAttribute("assignedSkillCounts", assignedSkillCounts); // For debugging
        model.addAttribute("hasCoveredSkills", hasCoveredSkills);

        // Debug logging
        log.debug("Project ID: {}", projectId);
        log.debug("Assigned employees: {}", assignedEmployees.size());
        log.debug("Assigned skill counts: {}", assignedSkillCounts);
        log.debug("Required counts: {}", requiredCounts);
        log.debug("Available counts: {}", availableCounts);
        log.debug("Skill gaps (calculated): {}", skillGaps);
        log.debug("Skill gaps (service): {}", serviceSkillGaps);
        log.debug("Coverage percentage (calculated): {}%", calculatedCoveragePercentage);
        log.debug("Coverage percentage (service): {}%", serviceSkillCoverage);

        return "resource-planner/project-staffing";
    }

    @GetMapping("/employees/{employeeId}/matching-projects")
    public String findMatchingProjects(
            @PathVariable Long employeeId,
            Model model) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Get all published and approved projects
        List<Project> publishedProjects = projectRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getPublished()))
                .filter(p -> p.getStatus() == ProjectStatus.APPROVED ||
                        p.getStatus() == ProjectStatus.STAFFING)
                .collect(Collectors.toList());

        // Find matching projects based on employee skills
        List<Project> matchingProjects = new ArrayList<>();

        for (Project project : publishedProjects) {
            if (project.getSkillRequirements() != null && !project.getSkillRequirements().isEmpty()) {
                for (ProjectSkillRequirement req : project.getSkillRequirements()) {
                    if (employee.getSkills() != null &&
                            employee.getSkills().contains(req.getSkill())) {
                        matchingProjects.add(project);
                        break; // Found at least one matching skill
                    }
                }
            }
        }

        model.addAttribute("employee", employee);
        model.addAttribute("matchingProjects", matchingProjects);
        model.addAttribute("totalMatches", matchingProjects.size());

        return "resource-planner/matching-projects";
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

        // USE THE VALIDATION SERVICE
        AssignmentValidationService.ValidationResult validation =
                validationService.canAssignEmployeeToProject(projectId, employeeId);

        if (validation.isError()) {
            redirectAttributes.addFlashAttribute("error",
                    employee.getName() + " - " + validation.getMessage());
            return "redirect:/ui/resource-planner/project/" + projectId;
        }

        // Create PENDING assignment
        Assignment pendingAssignment = new Assignment(project, employee, AssignmentStatus.PENDING);
        pendingAssignment.setAssignedAt(java.time.LocalDateTime.now());
        assignmentRepository.save(pendingAssignment);

        // ✅ FIXED: Employee STAYS available until they confirm
        // (Remove the lines that set employee.setAvailable(false))

        // ✅ ADD: Create notification for employee
        Notification notification = new Notification(
                employee.getId(),
                "New Assignment Proposed",
                "You have been proposed for project: " + project.getName() +
                        ". Please review and confirm your assignment in the Assignments section.",
                NotificationType.ASSIGNMENT_PROPOSED
        );
        notification.setRelatedAssignmentId(pendingAssignment.getId());
        notificationRepository.save(notification);

        // Log for workflow
        System.out.println("🔹 Resource Planner proposing employee " + employee.getName() +
                " for project " + project.getName() +
                (notes != null ? ". Notes: " + notes : ""));
        System.out.println("🔔 Notification created for employee ID: " + employee.getId());

        redirectAttributes.addFlashAttribute("success",
                "✅ Proposed " + employee.getName() + " for " + project.getName() +
                        ". Employee notified and awaiting confirmation.");

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

    // Add this method to ResourcePlannerUIController.java
    @PostMapping("/project/{projectId}/notify-pm-skill-gap")
    public String notifyPMSkillGap(
            @PathVariable Long projectId,
            @RequestParam(required = false) String message,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Check skill gaps
            Map<String, Integer> skillGaps = skillGapAnalysisService.getCriticalGaps(project);

            if (skillGaps.isEmpty()) {
                redirectAttributes.addFlashAttribute("warning",
                        "No skill gaps found. All requirements can be met internally.");
                return "redirect:/ui/resource-planner/project/" + projectId;
            }

            // Notify Project Manager (mark project for external search)
            project.setExternalSearchNeeded(true);
            project.setExternalSearchNotes("Project Manager notified about skill gaps: " +
                    skillGaps.keySet() + ". " + (message != null ? message : ""));
            project.setExternalSearchRequestedAt(java.time.LocalDateTime.now());
            project.setWorkflowStatus("AWAITING_PM_DECISION"); // NEW STATUS
            projectRepository.save(project);

            // Log notification
            String rpUsername = principal != null ? principal.getName() : "planner";
            log.info("🔔 Resource Planner {} notified PM {} about skill gaps for project: {}",
                    rpUsername, project.getCreatedBy(), project.getName());
            log.info("Missing skills: {}", skillGaps.keySet());

            redirectAttributes.addFlashAttribute("success",
                    "✅ Project Manager notified about skill gaps: " + skillGaps.keySet());

        } catch (Exception e) {
            log.error("Error notifying PM about skill gaps", e);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to notify Project Manager: " + e.getMessage());
        }

        return "redirect:/ui/resource-planner/project/" + projectId;
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