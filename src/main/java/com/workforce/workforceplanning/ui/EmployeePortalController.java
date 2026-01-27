package com.workforce.workforceplanning.ui;

import com.workforce.workforceplanning.model.*;
import com.workforce.workforceplanning.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Set;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Collections;



@Controller
@RequestMapping("/ui/employee")
public class EmployeePortalController {

    private final ProjectRepository projectRepository;
    private final AssignmentRepository assignmentRepository;
    private final ApplicationRepository applicationRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public EmployeePortalController(
            ProjectRepository projectRepository,
            AssignmentRepository assignmentRepository,
            ApplicationRepository applicationRepository,
            EmployeeRepository employeeRepository,
            NotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.assignmentRepository = assignmentRepository;
        this.applicationRepository = applicationRepository;
        this.employeeRepository = employeeRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // ==================== DASHBOARD ====================
    @GetMapping("/projects")
    public String showProjects(
            @RequestParam(required = false) String skill,
            Model model,
            Principal principal) {

        String username = principal != null ? principal.getName() : "Guest";
        model.addAttribute("username", username);

        // Get employee
        Employee employee = getEmployeeByUsername(username);

        // Get published projects
        List<Project> publishedProjects = projectRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getPublished()))
                .filter(p -> p.getStatus() == ProjectStatus.APPROVED ||
                        p.getStatus() == ProjectStatus.STAFFING)
                .collect(Collectors.toList());

        // ✅ FILTER OUT FULLY STAFFED PROJECTS
        List<Project> availableProjects = publishedProjects.stream()
                .filter(project -> {
                    // Count confirmed assignments for this project
                    long confirmedCount = assignmentRepository.findAll().stream()
                            .filter(a -> a.getProject().getId().equals(project.getId()))
                            .filter(a -> a.getStatus() == AssignmentStatus.CONFIRMED ||
                                    a.getStatus() == AssignmentStatus.IN_PROGRESS)
                            .count();

                    // Show project only if it needs more people
                    return confirmedCount < project.getTotalEmployeesRequired();
                })
                .collect(Collectors.toList());

        // Apply skill filter if provided
        if (skill != null && !skill.trim().isEmpty()) {
            availableProjects = availableProjects.stream()
                    .filter(p -> p.getSkillRequirements() != null &&
                            p.getSkillRequirements().stream()
                                    .anyMatch(req -> req.getSkill().toLowerCase()
                                            .contains(skill.toLowerCase().trim())))
                    .collect(Collectors.toList());
            model.addAttribute("searchSkill", skill);
        }

        model.addAttribute("projects", availableProjects);
        model.addAttribute("totalProjects", availableProjects.size());

        // Get employee's assignments and applications
        if (employee != null) {
            List<Assignment> myAssignments = assignmentRepository.findByEmployeeId(employee.getId());

            Set<Long> assignedProjectIds = myAssignments.stream()
                    .map(a -> a.getProject().getId())
                    .collect(Collectors.toSet());

            List<Application> myApplications = applicationRepository
                    .findByEmployeeIdOrderByAppliedAtDesc(employee.getId());

            Set<Long> appliedProjectIds = myApplications.stream()
                    .map(a -> a.getProject().getId())
                    .collect(Collectors.toSet());

            model.addAttribute("assignedProjectIds", assignedProjectIds);
            model.addAttribute("appliedProjectIds", appliedProjectIds);

            long unreadCount = notificationRepository
                    .countByEmployeeIdAndIsReadFalse(employee.getId());
            model.addAttribute("unreadNotifications", unreadCount);
        }

        return "projects-browse";
    }

    // ==================== 🔔 NOTIFICATIONS (PRIORITY #1) ====================
    @GetMapping("/notifications")
    public String showNotifications(Model model, Principal principal) {
        String username = principal.getName();
        Employee employee = getEmployeeByUsername(username);

        if (employee == null) {
            return "redirect:/ui/employee/projects?error=profile_not_found";
        }

        // Get all notifications
        List<Notification> allNotifications = notificationRepository
                .findByEmployeeIdOrderByCreatedAtDesc(employee.getId());

        // Separate unread and read
        List<Notification> unreadNotifications = allNotifications.stream()
                .filter(n -> !n.getIsRead())
                .collect(Collectors.toList());

        List<Notification> readNotifications = allNotifications.stream()
                .filter(Notification::getIsRead)
                .collect(Collectors.toList());

        long unreadCount = unreadNotifications.size();

        model.addAttribute("username", username);
        model.addAttribute("employee", employee);
        model.addAttribute("unreadNotifications", unreadNotifications);
        model.addAttribute("readNotifications", readNotifications);
        model.addAttribute("unreadCount", unreadCount);

        return "employee/notifications";
    }

    // Mark notification as read
    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String username = principal.getName();
        Employee employee = getEmployeeByUsername(username);

        try {
            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notification not found"));

            // Verify ownership
            if (!notification.getEmployeeId().equals(employee.getId())) {
                redirectAttributes.addFlashAttribute("error", "Unauthorized");
                return "redirect:/ui/employee/notifications";
            }

            notification.setIsRead(true);
            notificationRepository.save(notification);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to mark as read");
        }

        return "redirect:/ui/employee/notifications";
    }

    // Mark all as read
    @PostMapping("/notifications/read-all")
    public String markAllRead(Principal principal, RedirectAttributes redirectAttributes) {
        String username = principal.getName();
        Employee employee = getEmployeeByUsername(username);

        try {
            List<Notification> unreadNotifications = notificationRepository
                    .findByEmployeeIdAndIsReadFalseOrderByCreatedAtDesc(employee.getId());

            for (Notification notification : unreadNotifications) {
                notification.setIsRead(true);
                notificationRepository.save(notification);
            }

            redirectAttributes.addFlashAttribute("success",
                    "✅ All notifications marked as read");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "❌ Failed to mark all as read");
        }

        return "redirect:/ui/employee/notifications";
    }

    // ==================== 📋 MY ASSIGNMENTS (PRIORITY #2) ====================
    @GetMapping("/assignments")
    public String showAssignments(Model model, Principal principal) {
        String username = principal.getName();
        Employee employee = getEmployeeByUsername(username);

        if (employee == null) {
            return "redirect:/ui/employee/projects?error=profile_not_found";
        }

        // Get all assignments for this employee
        List<Assignment> allAssignments = assignmentRepository
                .findByEmployeeId(employee.getId());

        // Separate by status
        List<Assignment> pendingAssignments = allAssignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.PENDING)
                .collect(Collectors.toList());

        List<Assignment> confirmedAssignments = allAssignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.CONFIRMED)
                .collect(Collectors.toList());

        List<Assignment> inProgressAssignments = allAssignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.IN_PROGRESS)
                .collect(Collectors.toList());

        List<Assignment> completedAssignments = allAssignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.COMPLETED)
                .collect(Collectors.toList());

        List<Assignment> rejectedAssignments = allAssignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.REJECTED)
                .collect(Collectors.toList());

        model.addAttribute("username", username);
        model.addAttribute("employee", employee);
        model.addAttribute("pendingAssignments", pendingAssignments);
        model.addAttribute("confirmedAssignments", confirmedAssignments);
        model.addAttribute("inProgressAssignments", inProgressAssignments);
        model.addAttribute("completedAssignments", completedAssignments);
        model.addAttribute("rejectedAssignments", rejectedAssignments);

        // Get unread notifications count
        long unreadCount = notificationRepository
                .countByEmployeeIdAndIsReadFalse(employee.getId());
        model.addAttribute("unreadNotifications", unreadCount);

        return "employee/assignments";
    }

    // ==================== ✅ CONFIRM ASSIGNMENT (PRIORITY #3) ====================
    @PostMapping("/assignments/{id}/confirm")
    public String confirmAssignment(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String username = principal.getName();
        Employee employee = getEmployeeByUsername(username);

        try {
            Assignment assignment = assignmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Assignment not found"));

            // Verify ownership
            if (!assignment.getEmployee().getId().equals(employee.getId())) {
                redirectAttributes.addFlashAttribute("error",
                        "❌ This assignment does not belong to you");
                return "redirect:/ui/employee/assignments";
            }

            // Check status
            if (assignment.getStatus() != AssignmentStatus.PENDING) {
                redirectAttributes.addFlashAttribute("error",
                        "❌ Can only confirm PENDING assignments. Current status: " +
                                assignment.getStatus().getDisplayName());
                return "redirect:/ui/employee/assignments";
            }

            // CONFIRM ASSIGNMENT
            assignment.setStatus(AssignmentStatus.CONFIRMED);
            assignmentRepository.save(assignment);

            // SET EMPLOYEE AS UNAVAILABLE (PRIORITY #6)
            employee.setAvailable(false);
            employeeRepository.save(employee);

            redirectAttributes.addFlashAttribute("success",
                    "✅ Assignment confirmed! You are now assigned to: " +
                            assignment.getProject().getName());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "❌ Failed to confirm assignment: " + e.getMessage());
        }

        return "redirect:/ui/employee/assignments";
    }

    // ==================== ❌ REJECT ASSIGNMENT (PRIORITY #5) ====================
    @PostMapping("/assignments/{id}/reject")
    public String rejectAssignment(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String username = principal.getName();
        Employee employee = getEmployeeByUsername(username);

        try {
            // Get assignment
            Assignment assignment = assignmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Assignment not found"));

            // Verify ownership
            if (!assignment.getEmployee().getId().equals(employee.getId())) {
                redirectAttributes.addFlashAttribute("error",
                        "❌ You can only reject your own assignments");
                return "redirect:/ui/employee/assignments";
            }

            // Check status
            if (assignment.getStatus() != AssignmentStatus.PENDING) {
                redirectAttributes.addFlashAttribute("error",
                        "❌ Can only reject PENDING assignments");
                return "redirect:/ui/employee/assignments";
            }

            // REJECT ASSIGNMENT - Store full details
            assignment.setStatus(AssignmentStatus.REJECTED);
            assignment.setRejectedAt(LocalDateTime.now());
            assignment.setRejectedBy(employee.getName());

            String rejectReason = reason != null && !reason.trim().isEmpty()
                    ? reason
                    : "No reason provided";

            assignment.setRejectionReason(rejectReason);
            assignmentRepository.save(assignment);

            //  Notify Resource Planner
            notifyResourcePlannerOfRejection(assignment, employee, rejectReason);

            redirectAttributes.addFlashAttribute("success",
                    "❌ Assignment rejected: " + rejectReason);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "❌ Failed to reject assignment: " + e.getMessage());
        }

        return "redirect:/ui/employee/assignments";
    }


    // CORRECTED VERSION - Add these methods to EmployeePortalController.java

    @GetMapping("/profile")
    public String showProfile(Model model, Principal principal) {
        String username = principal.getName();

        // Get user details
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get employee details if linked
        Employee employee = user.getEmployee();  // ✅ FIXED

        // Get employee's applications
        List<Application> applications = Collections.emptyList();
        if (employee != null) {
            applications = applicationRepository.findByEmployeeIdOrderByAppliedAtDesc(employee.getId());
        }

        // Get employee's assignments
        List<Assignment> assignments = Collections.emptyList();
        if (employee != null) {
            assignments = assignmentRepository.findByEmployeeId(employee.getId());
        }

        model.addAttribute("user", user);
        model.addAttribute("employee", employee);
        model.addAttribute("applications", applications);
        model.addAttribute("assignments", assignments);

        return "employees/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String name,
                                @RequestParam String email,
                                @RequestParam(required = false) String skills,
                                @RequestParam(required = false, defaultValue = "0") Integer experience,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            String username = principal.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Employee employee = user.getEmployee();
            if (employee != null) {

                // Update employee details
                employee.setName(name);
                employee.setEmail(email);

                // Parse and update skills
                if (skills != null && !skills.trim().isEmpty()) {
                    Set<String> skillSet = Arrays.stream(skills.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toSet());
                    employee.setSkills(skillSet);
                }

                employeeRepository.save(employee);
                redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "No employee record found for this user");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update profile: " + e.getMessage());
        }

        return "redirect:/ui/employee/profile";
    }

    // ==================== 📝 APPLY FOR PROJECT (PRIORITY #4) ====================
    @PostMapping("/projects/{projectId}/apply")
    public String applyForProject(
            @PathVariable Long projectId,
            @RequestParam(required = false) String message,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String username = principal.getName();
        Employee employee = getEmployeeByUsername(username);

        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Check if already applied
            boolean alreadyApplied = applicationRepository
                    .existsByProjectIdAndEmployeeId(projectId, employee.getId());

            if (alreadyApplied) {
                redirectAttributes.addFlashAttribute("error",
                        "❌ You have already applied to this project");
                return "redirect:/ui/employee/projects";
            }

            // Check if already assigned
            boolean alreadyAssigned = assignmentRepository
                    .existsByProjectIdAndEmployeeId(projectId, employee.getId());

            if (alreadyAssigned) {
                redirectAttributes.addFlashAttribute("error",
                        "❌ You are already assigned to this project");
                return "redirect:/ui/employee/projects";
            }

            // Create application
            Application application = new Application();
            application.setProject(project);
            application.setEmployee(employee);
            application.setMessage(message);
            application.setStatus(ApplicationStatus.PENDING);
            applicationRepository.save(application);

            redirectAttributes.addFlashAttribute("success",
                    "✅ Application submitted successfully for: " + project.getName());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "❌ Failed to apply: " + e.getMessage());
        }

        return "redirect:/ui/employee/projects";
    }

    // ==================== 📄 VIEW MY APPLICATIONS ====================
    @GetMapping("/applications")
    public String showApplications(Model model, Principal principal) {
        String username = principal.getName();
        Employee employee = getEmployeeByUsername(username);

        if (employee == null) {
            return "redirect:/ui/employee/projects?error=profile_not_found";
        }

        List<Application> applications = applicationRepository
                .findByEmployeeIdOrderByAppliedAtDesc(employee.getId());

        model.addAttribute("username", username);
        model.addAttribute("employee", employee);
        model.addAttribute("applications", applications);

        // Get unread notifications count
        long unreadCount = notificationRepository
                .countByEmployeeIdAndIsReadFalse(employee.getId());
        model.addAttribute("unreadNotifications", unreadCount);

        return "employee/applications";
    }

    // ==================== HELPER METHOD ====================
    private Employee getEmployeeByUsername(String username) {
        return employeeRepository.findAll().stream()
                .filter(e -> username.equals(e.getEmail().split("@")[0]))
                .findFirst()
                .orElse(null);
    }

    /**
     * Notify Resource Planner when employee rejects assignment
     */
    private void notifyResourcePlannerOfRejection(Assignment assignment, Employee employee, String reason) {
        try {
            // Find Resource Planner user
            User resourcePlanner = userRepository.findAll().stream()
                    .filter(u -> u.getRole() != null &&
                            (u.getRole().equals("RESOURCE_PLANNER") ||
                                    u.getRole().equals("ROLE_RESOURCE_PLANNER")))
                    .findFirst()
                    .orElse(null);

            if (resourcePlanner == null) {
                System.out.println("⚠️ No Resource Planner found - notification not sent");
                return;
            }

            // Get Resource Planner's employee record
            Employee rpEmployee = resourcePlanner.getEmployee();
            if (rpEmployee == null) {
                System.out.println("⚠️ Resource Planner has no employee record - notification not sent");
                return;
            }

            // Create notification
            Notification notification = new Notification();
            notification.setEmployeeId(rpEmployee.getId());
            notification.setType(NotificationType.ASSIGNMENT_REJECTED);
            notification.setIsRead(false);

            // Notification message
            String message = String.format(
                    "🚫 %s rejected assignment to '%s'. Reason: %s",
                    employee.getName(),
                    assignment.getProject().getName(),
                    reason
            );
            notification.setMessage(message);

            // Save notification
            notificationRepository.save(notification);

            System.out.println("✅ Notification sent to Resource Planner: " + message);

        } catch (Exception e) {
            System.err.println("❌ Failed to notify Resource Planner: " + e.getMessage());
            e.printStackTrace();
        }
    }
}