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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.Principal;
import java.util.*;
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
    private final UserRepository userRepository;

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
            SkillGapAnalysisService skillGapAnalysisService,
            UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
        this.assignmentRepository = assignmentRepository;
        this.applicationRepository = applicationRepository;
        this.externalSearchService = externalSearchService;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.skillGapAnalysisService = skillGapAnalysisService;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model,
                            @RequestParam(value = "view", required = false) String view, Principal principal) {

        // Default view
        String activeView = (view != null) ? view : "projects";

        try {
            // Get published & approved projects
            List<Project> availableProjects = projectRepository.findAll().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getPublished()))
                    .filter(p -> p.getStatus() == ProjectStatus.APPROVED || p.getStatus() == ProjectStatus.STAFFING)
                    .collect(Collectors.toList());
            System.out.println("\n=== DEBUG: Dashboard - Available Projects ===");
            System.out.println("Total available projects: " + availableProjects.size());

            // Get available employees
//            List<Employee> availableEmployees = employeeRepository.findAll().stream()
//                    .filter(e -> Boolean.TRUE.equals(e.getAvailable()))
//                    .collect(Collectors.toList());

            // Get available employees WITHOUT pending assignments
            List<Assignment> pendingAssignments = assignmentRepository.findAll().stream()
                    .filter(a -> a.getStatus() == AssignmentStatus.PENDING)
                    .collect(Collectors.toList());

            Set<Long> employeesWithPending = pendingAssignments.stream()
                    .map(a -> a.getEmployee().getId())
                    .collect(Collectors.toSet());

            List<Employee> availableEmployees = employeeRepository.findAll().stream()
                    .filter(e -> Boolean.TRUE.equals(e.getAvailable()))
                    .filter(e -> !employeesWithPending.contains(e.getId()))
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
                // Count only CONFIRMED/IN_PROGRESS assignments
                long confirmedCount = allAssignments.stream()
                        .filter(a -> a.getProject().getId().equals(project.getId()))
                        .filter(a -> a.getStatus() == AssignmentStatus.CONFIRMED ||
                                a.getStatus() == AssignmentStatus.IN_PROGRESS)
                        .count();

                // Count PENDING assignments
                long pendingCount = allAssignments.stream()
                        .filter(a -> a.getProject().getId().equals(project.getId()))
                        .filter(a -> a.getStatus() == AssignmentStatus.PENDING)
                        .count();

                int required = project.getTotalEmployeesRequired();

                // Calculate progress percentage
                double progress = 0.0;
                if (required > 0) {
                    // Base progress: 50% when project approved (ready to staff)
                    progress = 50.0;

                    // Add 30% for pending assignments (10% per pending, max 30%)
                    if (pendingCount > 0) {
                        progress += Math.min(30.0, (pendingCount * 10.0));
                    }

                    // Add remaining to 100% based on confirmed
                    double confirmedProgress = ((double) confirmedCount / required) * 50.0;
                    progress += confirmedProgress;

                    // Cap at 100%
                    progress = Math.min(100.0, progress);
                }


                // Find matching available employees
                List<Employee> matchingEmployees = findMatchingEmployees(project);


                projectStaffingInfo.put(project.getId(),
                        new StaffingInfo((int) confirmedCount, matchingEmployees.size(), progress));
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
            // ✅ ADD THIS NOTIFICATION CODE HERE (BEFORE THE RETURN!)
            long unreadCount = 0;
            try {
                String username = principal != null ? principal.getName() : null;
                if (username != null) {
                    User user = userRepository.findByUsername(username).orElse(null);
                    if (user != null && user.getEmployee() != null) {
                        unreadCount = notificationRepository
                                .countByEmployeeIdAndIsReadFalse(user.getEmployee().getId());
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to get notification count: " + e.getMessage());
            }

            model.addAttribute("unreadNotifications", unreadCount);

            return "resource-planner/dashboard";


        } catch (Exception e) {
            // If there's an error, return empty lists
            model.addAttribute("availableProjects", List.of());
            model.addAttribute("availableEmployees", List.of());
            model.addAttribute("activeView", activeView);
            model.addAttribute("unreadNotifications", 0);
            return "resource-planner/dashboard";
        }

    }

    @GetMapping("/project/{projectId}")
    public String viewProjectStaffing(@PathVariable("projectId") Long projectId, Model model) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Get current assignments for this project (exclude REJECTED)
        List<Assignment> currentAssignments = assignmentRepository.findAll().stream()
                .filter(a -> a.getProject().getId().equals(projectId))
                .filter(a -> a.getStatus() != AssignmentStatus.REJECTED)
                .collect(Collectors.toList());

        // Find matching employees (available employees with matching skills)
        List<Employee> matchingEmployees = findMatchingEmployees(project);

        // Get rejection history for this project
        List<Assignment> rejectedAssignments = assignmentRepository.findAll().stream()
                .filter(a -> a.getProject().getId().equals(projectId))
                .filter(a -> a.getStatus() == AssignmentStatus.REJECTED)
                .collect(Collectors.toList());

        model.addAttribute("rejectedAssignments", rejectedAssignments);
        model.addAttribute("hasRejections", !rejectedAssignments.isEmpty());

        // Get pending applications for this project
        List<Application> pendingApplications = applicationRepository.findAll().stream()
                .filter(app -> app.getProject().getId().equals(projectId))
                .filter(app -> app.getStatus() == ApplicationStatus.PENDING)
                .collect(Collectors.toList());

        // Get all skills for filter
        Set<String> allSkills = employeeRepository.findAll().stream()
                .flatMap(e -> e.getSkills().stream())
                .collect(Collectors.toSet());

        // ----- CHECK IF SKILLS ARE AVAILABLE INTERNALLY -----
        boolean skillsAvailableInternally = areSkillsAvailableInternally(project);
        model.addAttribute("skillsAvailableInternally", skillsAvailableInternally);

        // ----- CALCULATE SKILL AVAILABILITY (Required vs Available) -----
        Map<String, Map<String, Object>> skillAvailabilityDetails = new LinkedHashMap<>();

        if (project.getSkillRequirements() != null) {
            // Get all available employees
            List<Employee> allAvailableEmployees = employeeRepository.findAll().stream()
                    .filter(e -> Boolean.TRUE.equals(e.getAvailable()))
                    .collect(Collectors.toList());

            for (ProjectSkillRequirement req : project.getSkillRequirements()) {
                String skill = req.getSkill();
                int required = req.getRequiredCount();

                // Count available employees with this skill
                List<Employee> employeesWithSkill = allAvailableEmployees.stream()
                        .filter(e -> e.getSkills().stream()
                                .anyMatch(s -> s.equalsIgnoreCase(skill)))
                        .collect(Collectors.toList());

                int available = employeesWithSkill.size();
                boolean hasGap = available < required;
                int gap = Math.max(0, required - available);

                // Create detailed skill info
                Map<String, Object> skillInfo = new HashMap<>();
                skillInfo.put("skill", skill);
                skillInfo.put("required", required);
                skillInfo.put("available", available);
                skillInfo.put("hasGap", hasGap);
                skillInfo.put("gap", gap);
                skillInfo.put("employeesWithSkill", employeesWithSkill.stream()
                        .map(Employee::getName)
                        .collect(Collectors.toList()));
                skillInfo.put("isAvailable", available >= required);

                skillAvailabilityDetails.put(skill, skillInfo);
            }
        }

        model.addAttribute("skillAvailabilityDetails", skillAvailabilityDetails);

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

        /// Calculate coverage percentage
        int totalSkillsNeeded = 0;
        int skillsCovered = 0;

        int totalPositionsNeeded = 0;
        int positionsFilled = 0;

        if (project.getSkillRequirements() != null) {
            // Track which skills have been satisfied by which employees
            Map<String, Integer> remainingSkillRequirements = new HashMap<>();
            List<Employee> remainingEmployees = new ArrayList<>(assignedEmployees);

            // Initialize with required counts
            for (ProjectSkillRequirement req : project.getSkillRequirements()) {
                String reqSkill = req.getSkill().toLowerCase().trim();
                remainingSkillRequirements.put(reqSkill, req.getRequiredCount());
                totalPositionsNeeded += req.getRequiredCount();
            }

            for (Employee emp : assignedEmployees) {
                if (emp.getSkills() != null) {
                    // Find the first remaining skill requirement this employee can satisfy
                    for (String empSkill : emp.getSkills()) {
                        String empSkillLower = empSkill.toLowerCase().trim();
                        if (remainingSkillRequirements.containsKey(empSkillLower)
                                && remainingSkillRequirements.get(empSkillLower) > 0) {
                            // This employee can fill this skill position
                            remainingSkillRequirements.put(empSkillLower,
                                    remainingSkillRequirements.get(empSkillLower) - 1);
                            positionsFilled++;
                            break; // Employee can only fill one position
                        }
                    }
                }
            }
        }

        // Calculate coverage based on assigned employees
//        double calculatedCoveragePercentage = 0;
//        if (totalSkillsNeeded > 0) {
//            calculatedCoveragePercentage = ((double) skillsCovered / totalSkillsNeeded) * 100;
//        }
        double calculatedCoveragePercentage = 0;
        if (totalPositionsNeeded > 0) {
            calculatedCoveragePercentage = ((double) positionsFilled / totalPositionsNeeded) * 100;
        }
        // ----- SKILL GAP CALCULATION (Based on assigned employees) -----

        Map<String, Integer> skillGaps = new HashMap<>();
        if (project.getSkillRequirements() != null) {
            // Track remaining needs - initialize with required counts
            Map<String, Integer> remainingNeeds = new HashMap<>();
            for (ProjectSkillRequirement req : project.getSkillRequirements()) {
                remainingNeeds.put(req.getSkill().toLowerCase().trim(), req.getRequiredCount());
            }

            // For each assigned employee, fill one position
            for (Employee emp : assignedEmployees) {
                if (emp.getSkills() != null) {
                    for (String empSkill : emp.getSkills()) {
                        String empSkillLower = empSkill.toLowerCase().trim();
                        if (remainingNeeds.containsKey(empSkillLower) && remainingNeeds.get(empSkillLower) > 0) {
                            remainingNeeds.put(empSkillLower, remainingNeeds.get(empSkillLower) - 1);
                            break; // Each employee fills only one position
                        }
                    }
                }
            }

            // Convert remaining needs to skill gaps
            for (ProjectSkillRequirement req : project.getSkillRequirements()) {
                String reqSkill = req.getSkill().toLowerCase().trim();
                int gap = remainingNeeds.getOrDefault(reqSkill, 0);
                if (gap > 0) {
                    skillGaps.put(req.getSkill(), gap);
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
        if (project.getSkillRequirements() != null && skillGaps != null) {
            for (ProjectSkillRequirement req : project.getSkillRequirements()) {
                String skill = req.getSkill();
                if (!skillGaps.containsKey(skill) || skillGaps.get(skill) <= 0) {
                    coveredSkillsCount++;
                }
            }
        }

        // ----- EXTERNAL SEARCH & PM NOTIFICATION -----

        // Check if PM already notified
//        boolean pmNotified = Boolean.TRUE.equals(project.getExternalSearchNeeded()) &&
//                "AWAITING_PM_DECISION".equals(project.getWorkflowStatus());

        boolean pmNotified = "AWAITING_PM_DECISION".equals(project.getWorkflowStatus());

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
        model.addAttribute("totalPositionsNeeded", totalPositionsNeeded);
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

    private boolean areSkillsAvailableInternally(Project project) {
        if (project.getSkillRequirements() == null || project.getSkillRequirements().isEmpty()) {
            return true; // No skill requirements = skills are "available"
        }

        // Get all available employees
        List<Employee> availableEmployees = employeeRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getAvailable()))
                .collect(Collectors.toList());

        // Check if all required skills are available
        for (ProjectSkillRequirement req : project.getSkillRequirements()) {
            String requiredSkill = req.getSkill().toLowerCase().trim();
            int requiredCount = req.getRequiredCount();

            // Count employees with this skill
            long employeesWithSkill = availableEmployees.stream()
                    .filter(e -> e.getSkills().stream()
                            .anyMatch(skill -> skill.toLowerCase().trim().equals(requiredSkill)))
                    .count();

            // If we don't have enough employees with this skill
            if (employeesWithSkill < requiredCount) {
                return false; // Skill gap exists
            }
        }

        return true; // All skills are available
    }

    @GetMapping("/employees/{employeeId}/matching-projects")
    public String findMatchingProjects(
            @PathVariable("employeeId") Long employeeId,
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

    @GetMapping("/staffing-tracker")
    public String trackStaffing(Model model) {
        // Get all confirmed/in-progress assignments
        List<Assignment> activeAssignments = assignmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == AssignmentStatus.CONFIRMED ||
                        a.getStatus() == AssignmentStatus.IN_PROGRESS)
                .collect(Collectors.toList());

        // Group by project
        Map<Project, List<Employee>> projectStaffing = activeAssignments.stream()
                .collect(Collectors.groupingBy(
                        Assignment::getProject,
                        Collectors.mapping(Assignment::getEmployee, Collectors.toList())
                ));

        model.addAttribute("projectStaffing", projectStaffing);
        model.addAttribute("totalProjects", projectStaffing.size());
        model.addAttribute("totalEmployees", activeAssignments.size());

        return "resource-planner/staffing-tracker";
    }

    @GetMapping("/notifications")
    public String viewNotifications(Model model, Principal principal) {
        String username = principal.getName();

        // Find Resource Planner's employee record
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Employee rpEmployee = user.getEmployee();

        if (rpEmployee == null) {
            model.addAttribute("error", "No employee record found");
            model.addAttribute("notifications", List.of());
            model.addAttribute("unreadCount", 0);
            return "resource-planner/notifications";
        }

        // Get all notifications for Resource Planner
        List<Notification> allNotifications = notificationRepository
                .findByEmployeeIdOrderByCreatedAtDesc(rpEmployee.getId());

        // Separate unread and read
        List<Notification> unreadNotifications = allNotifications.stream()
                .filter(n -> !n.getIsRead())
                .collect(Collectors.toList());

        List<Notification> readNotifications = allNotifications.stream()
                .filter(Notification::getIsRead)
                .collect(Collectors.toList());

        model.addAttribute("username", username);
        model.addAttribute("unreadNotifications", unreadNotifications);
        model.addAttribute("readNotifications", readNotifications);
        model.addAttribute("unreadCount", unreadNotifications.size());

        return "resource-planner/notifications";
    }

    // Add mark as read endpoint
    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notification not found"));

            notification.setIsRead(true);
            notificationRepository.save(notification);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to mark as read");
        }

        return "redirect:/ui/resource-planner/notifications";
    }

    // Add mark all as read
    @PostMapping("/notifications/read-all")
    public String markAllRead(Principal principal, RedirectAttributes redirectAttributes) {
        String username = principal.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user != null && user.getEmployee() != null) {
            List<Notification> unreadNotifications = notificationRepository
                    .findByEmployeeIdAndIsReadFalseOrderByCreatedAtDesc(user.getEmployee().getId());

            unreadNotifications.forEach(n -> n.setIsRead(true));
            notificationRepository.saveAll(unreadNotifications);
        }

        return "redirect:/ui/resource-planner/notifications";
    }

    @PostMapping("/project/{projectId}/propose")
    public String proposeEmployee(
            @PathVariable("projectId") Long projectId,
            @RequestParam("employeeId") Long employeeId,
            @RequestParam(value = "notes", required = false) String notes,
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
                employee.getId(),  // This is correct for employee notifications
                "New Assignment Proposed",
                "You have been proposed for project: " + project.getName() +
                        ". Please totalCount and confirm your assignment in the Assignments section.",
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
            @RequestParam(value = "skills", required = false) String skills,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "available", required = false) Boolean available,
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
            @PathVariable("assignmentId") Long assignmentId,
            @RequestParam(value = "reason", required = false) String reason,
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
    private List<Employee> findMatchingEmployees(Project project) {
        // Get all available employees
        List<Employee> allAvailable = employeeRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getAvailable()))
                .collect(Collectors.toList());

        // Filter out employees with pending assignments
        List<Employee> availableWithoutPending = allAvailable.stream()
                .filter(e -> !hasPendingAssignments(e.getId()))
                .collect(Collectors.toList());

        if (project.getSkillRequirements() == null || project.getSkillRequirements().isEmpty()) {
            return availableWithoutPending;
        }

        // Get required skills
        Set<String> requiredSkills = project.getSkillRequirements().stream()
                .map(req -> req.getSkill().toLowerCase().trim())
                .collect(Collectors.toSet());

        // Filter by skills
        return availableWithoutPending.stream()
                .filter(e -> {
                    if (e.getSkills() == null || e.getSkills().isEmpty()) {
                        return false;
                    }
                    Set<String> employeeSkills = e.getSkills().stream()
                            .map(skill -> skill.toLowerCase().trim())
                            .collect(Collectors.toSet());
                    return requiredSkills.stream()
                            .anyMatch(employeeSkills::contains);
                })
                .sorted((e1, e2) -> {
                    // Sort by matching skill count
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
    public String cancelProposal(@PathVariable("assignmentId") Long assignmentId) {
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
            @PathVariable("id") Long id,
            @RequestParam(value = "notes", required = false) String notes,
            RedirectAttributes redirectAttributes) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Mark project for external search
        project.setExternalSearchNeeded(true);
        project.setExternalSearchNotes(notes);
        project.setExternalSearchRequestedAt(java.time.LocalDateTime.now());
        projectRepository.save(project);


        redirectAttributes.addFlashAttribute("success",
                "External search initiated for project: " + project.getName() +
                        ". Recruitment team has been notified.");

        return "redirect:/ui/resource-planner/project/" + id;
    }

    @GetMapping("/project/{id}/external-candidates")
    public String viewExternalCandidates(@PathVariable("id") Long id, Model model) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        model.addAttribute("project", project);

        // TODO: In real implementation, fetch from external APIs
        List<Map<String, String>> externalCandidates = new ArrayList<>();


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
    @PostMapping("/project/{projectId}/notify-pm-skill-gap")
    public String notifyPMSkillGap(
            @PathVariable("projectId") Long projectId,
            @RequestParam(value = "message", required = false) String message,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("=== DEBUG: notifyPMSkillGap START ===");
            log.info("Project ID: {}", projectId);
            log.info("Message: {}", message);

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            log.info("Project found: {}", project.getName());
            log.info("Created by (PM): {}", project.getCreatedBy());
            log.info("Current workflowStatus: {}", project.getWorkflowStatus());
            log.info("Current externalSearchNeeded: {}", project.getExternalSearchNeeded());

            // Check if PM has already been notified
            if (Boolean.TRUE.equals(project.getExternalSearchNeeded()) &&
                    "AWAITING_PM_DECISION".equals(project.getWorkflowStatus())) {
                log.info("⚠️ PM already notified about skill gaps");
                redirectAttributes.addFlashAttribute("warning",
                        "Project Manager has already been notified about skill gaps.");
                return "redirect:/ui/resource-planner/project/" + projectId;
            }

            // Check skill gaps
            Map<String, Integer> skillGaps = skillGapAnalysisService.getCriticalGaps(project);
            log.info("Skill gaps found: {}", skillGaps);

            if (skillGaps.isEmpty()) {
                redirectAttributes.addFlashAttribute("warning",
                        "No skill gaps found. All requirements can be met internally.");
                return "redirect:/ui/resource-planner/project/" + projectId;
            }

            // Notify Project Manager (mark project for external search)
            project.setExternalSearchNeeded(false);
            project.setExternalSearchNotes("Project Manager notified about skill gaps: " +
                    skillGaps.keySet() + ". " + (message != null ? message : ""));
            project.setExternalSearchRequestedAt(java.time.LocalDateTime.now());
            project.setWorkflowStatus("AWAITING_PM_DECISION"); // NEW STATUS
            project.setPmNotificationSeen(false); // PM hasn't seen this notification yet

            log.info("Setting externalSearchNeeded to: true");
            log.info("Setting workflowStatus to: AWAITING_PM_DECISION");
            log.info("Setting pmNotificationSeen to: false");

            // ========== NOTIFICATION FOR PM ==========
            String skillGapMessage = skillGaps.entrySet().stream()
                    .map(entry -> entry.getKey() + " (missing " + entry.getValue() + " employee" +
                            (entry.getValue() > 1 ? "s" : "") + ")")
                    .collect(Collectors.joining(", "));

            String pmUsername = project.getCreatedBy();

            // Create notification for PM using username constructor
            Notification pmNotification = new Notification(
                    pmUsername,  // PM's username (String)
                    "Skill Gap Alert - " + project.getName(),
                    "Resource Planner found skill gaps in your project: " + skillGapMessage +
                            (message != null ? ". Note from Resource Planner: " + message : ""),
                    NotificationType.ASSIGNMENT_PROPOSED
            );
            pmNotification.setProjectId(projectId);
            pmNotification.setProjectName(project.getName());

            notificationRepository.save(pmNotification);
            log.info("✅ Notification saved for PM: {}", pmUsername);

            // Save project changes
            projectRepository.save(project);
            log.info("✅ Project saved with new workflow status");

            log.info("=== DEBUG: notifyPMSkillGap END ===");

            // Success message - this will be displayed on the redirected page
            redirectAttributes.addFlashAttribute("success",
                    "✅ Project Manager has been notified about skill gaps! They will see a notification when they log in.");

        } catch (Exception e) {
            log.error("❌ Error notifying PM about skill gaps", e);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to notify Project Manager: " + e.getMessage());
        }

        return "redirect:/ui/resource-planner/project/" + projectId;
    }


    // Helper class for staffing information
    private static class StaffingInfo {
        int confirmedCount;
        int matchingEmployeesCount;
        double progress;

        StaffingInfo(int assignedCount, int matchingEmployeesCount, double progress) {
            this.confirmedCount = assignedCount;
            this.matchingEmployeesCount = matchingEmployeesCount;
            this.progress = progress;
        }

        public int getAssignedCount() { return confirmedCount; }
        public int getMatchingEmployeesCount() { return matchingEmployeesCount; }
        public double getProgress() { return progress; }
    }

    private boolean hasPendingAssignments(Long employeeId) {
        return assignmentRepository.findAll().stream()
                .anyMatch(a -> a.getEmployee().getId().equals(employeeId)
                        && a.getStatus() == AssignmentStatus.PENDING);
    }
}