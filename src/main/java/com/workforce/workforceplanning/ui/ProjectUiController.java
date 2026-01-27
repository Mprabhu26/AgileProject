package com.workforce.workforceplanning.ui;

import com.workforce.workforceplanning.dto.CreateProjectForm;
import com.workforce.workforceplanning.dto.SkillRequirementDto;
import com.workforce.workforceplanning.model.*;
import com.workforce.workforceplanning.repository.*;
import com.workforce.workforceplanning.service.ExternalSearchService;
import com.workforce.workforceplanning.service.SkillGapAnalysisService;
import com.workforce.workforceplanning.service.UserRoleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.workforce.workforceplanning.workflow.ExternalSearchApprovalDelegate;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.workforce.workforceplanning.repository.NotificationRepository;

@Controller
@RequestMapping("/ui/projects")
public class ProjectUiController {

    private final ProjectRepository projectRepository;
    private final AssignmentRepository assignmentRepository;
    private final ApplicationRepository applicationRepository;
    private final EmployeeRepository employeeRepository;
    private final ExternalSearchService externalSearchService;
    private final SkillGapAnalysisService skillGapAnalysisService;
    private final NotificationRepository notificationRepository;
    private final UserRoleService userRoleService;


    public ProjectUiController(
            ProjectRepository projectRepository,
            AssignmentRepository assignmentRepository,
            ApplicationRepository applicationRepository,
            ExternalSearchService externalSearchService,
            SkillGapAnalysisService skillGapAnalysisService,
            EmployeeRepository employeeRepository,
            NotificationRepository notificationRepository, UserRoleService userRoleService){
        this.projectRepository = projectRepository;
        this.assignmentRepository = assignmentRepository;
        this.applicationRepository = applicationRepository;
        this.employeeRepository = employeeRepository;
        this.externalSearchService = externalSearchService;
        this.skillGapAnalysisService = skillGapAnalysisService;
        this.notificationRepository = notificationRepository;
        this.userRoleService = userRoleService;
    }

    // ==================== PROJECT LIST ====================
    @GetMapping
    public String myProjects(Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Guest";
        model.addAttribute("username", username);

        List<Project> projects = projectRepository.findByCreatedBy(username);

        // Calculate statistics
        long totalCount = projects.size();
        long approvalpendingCount = projects.stream().filter(p -> p.getStatus() == ProjectStatus.PENDING_APPROVAL).count();
        long externalpendingcount =projects.stream().filter(p -> p.getStatus() == ProjectStatus.STAFFING).count();
        long pendingCount=approvalpendingCount + externalpendingcount;
        long approvedCount = projects.stream().filter(p -> p.getStatus() == ProjectStatus.APPROVED).count();
        long publishedCount = projects.stream().filter(p -> Boolean.TRUE.equals(p.getPublished())).count();
        long draftCount = projects.stream().filter(p -> p.getStatus() == ProjectStatus.DRAFT).count();
        long rejectCount = projects.stream().filter(p -> p.getStatus() == ProjectStatus.REJECTED).count();
        long staffingCount = projects.stream().filter(p -> p.getStatus() == ProjectStatus.STAFFING).count();

        // KEEP EXISTING PROJECT-BASED NOTIFICATIONS
        List<Map<String, Object>> projectNotifications = new ArrayList<>();

        for (Project project : projects) {
            // Notification 1: Resource Planner found skill gaps
            if ("AWAITING_PM_DECISION".equals(project.getWorkflowStatus())
                    && Boolean.FALSE.equals(project.getPmNotificationSeen())) {


                Map<String, Object> notif = new HashMap<>();
                notif.put("id", "project-" + project.getId()); // Use string ID to differentiate
                notif.put("projectId", project.getId());
                notif.put("projectName", project.getName());

                // Determine message based on workflow status
                String workflowStatus = project.getWorkflowStatus();
                if ("AWAITING_PM_DECISION".equals(workflowStatus)) {
                    notif.put("message", "Resource Planner found skill gaps. External search needed.");
                    notif.put("priority", "high");
                } else if ("AWAITING_DEPARTMENT_HEAD_APPROVAL".equals(workflowStatus)) {
                    notif.put("message", "External search awaiting Department Head approval");
                    notif.put("priority", "medium");
                } else if ("EXTERNAL_SEARCH_APPROVED".equals(workflowStatus)) {
                    notif.put("message", "External search approved! Resource Planner will search.");
                    notif.put("priority", "low");
                } else if ("EXTERNAL_SEARCH_REJECTED".equals(workflowStatus)) {
                    notif.put("message", "External search rejected. Please review.");
                    notif.put("priority", "high");
                }

                notif.put("isDbNotification", false);
                notif.put("createdAt", project.getExternalSearchRequestedAt() != null
                        ? project.getExternalSearchRequestedAt()
                        : LocalDateTime.now());
                notif.put("isRead", false);
                projectNotifications.add(notif);
            }
        }

        //DATABASE NOTIFICATIONS (FROM RESOURCE PLANNER)
        List<Notification> dbNotifications = notificationRepository
                .findByUsernameAndIsReadFalseOrderByCreatedAtDesc(username);

        // Combine both types of notifications
        List<Map<String, Object>> allNotifications = new ArrayList<>();

        // Add project notifications first
        allNotifications.addAll(projectNotifications);

        // Add database notifications
        for (Notification notification : dbNotifications) {
            Map<String, Object> notif = new HashMap<>();
            notif.put("id", notification.getId());
            notif.put("title", notification.getTitle() != null ? notification.getTitle() : "Notification");
            notif.put("message", notification.getMessage());
            notif.put("projectId", notification.getProjectId());
            notif.put("projectName", notification.getProjectName());
            notif.put("createdAt", notification.getCreatedAt());
            notif.put("isRead", notification.getIsRead());
            notif.put("isDbNotification", true); // Mark as DB notification
            allNotifications.add(notif);
        }

        model.addAttribute("projects", projects);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("draftCount", draftCount);
        model.addAttribute("rejectCount", rejectCount);
        model.addAttribute("staffingCount", staffingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("publishedCount", publishedCount);
        model.addAttribute("notifications", allNotifications); // Combined list
        model.addAttribute("notificationCount", allNotifications.size()); // Total count

        // Get pending applications count
        List<Application> pendingApplications = applicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.PENDING)
                .toList();
        model.addAttribute("pendingApplicationsCount", pendingApplications.size());

        return "projects/dashboard";
    }

    // ==================== CREATE PROJECT ====================
    @GetMapping("/create")
    public String showCreateForm(Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Guest";
        model.addAttribute("username", username);
        model.addAttribute("projectForm", new CreateProjectForm());
        return "projects/create";
    }

    @PostMapping("/create")
    public String createProject(
            @ModelAttribute CreateProjectForm form,
            Principal principal,
            RedirectAttributes redirectAttributes) { // ✅ ADDED RedirectAttributes
        try {
            String username = principal != null ? principal.getName() : "Guest";

            Project project = new Project();
            project.setName(form.getName());
            project.setDescription(form.getDescription());
            project.setStartDate(form.getStartDate());
            project.setEndDate(form.getEndDate());
            project.setBudget(form.getBudget());
            project.setTotalEmployeesRequired(form.getTotalEmployeesRequired());
            project.setCreatedBy(username);
            project.setStatus(ProjectStatus.DRAFT);

            // Initialize skill requirements list
            project.setSkillRequirements(new ArrayList<>());

            // Handle skill requirements
            if (form.getSkillRequirements() != null) {
                for (SkillRequirementDto dto : form.getSkillRequirements()) {
                    if (dto != null && dto.getSkill() != null && !dto.getSkill().trim().isEmpty()) {
                        Integer count = dto.getRequiredCount();
                        if (count == null || count < 1) {
                            count = 1;
                        }

                        ProjectSkillRequirement requirement = new ProjectSkillRequirement();
                        requirement.setSkill(dto.getSkill().trim());
                        requirement.setRequiredCount(count);
                        requirement.setProject(project);

                        project.getSkillRequirements().add(requirement);
                    }
                }
            }

            projectRepository.save(project);

            // ✅ ADD SUCCESS MESSAGE
            redirectAttributes.addFlashAttribute("successMessage",
                    "Project '" + project.getName() + "' created successfully!");

            // ✅ REDIRECT TO DASHBOARD INSTEAD OF LIST
            return "redirect:/ui/projects/dashboard";

        } catch (Exception e) {
            // ✅ ADD ERROR MESSAGE
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error creating project: " + e.getMessage());
            return "redirect:/ui/projects/create";
        }
    }

    // ==================== VIEW PROJECT DETAILS ====================
    @GetMapping("/{id}")
    public String viewProject(@PathVariable("id") Long id, Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Guest";

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        markAllProjectNotificationsAsRead(id, username);

        // Check if the project is published or if the user is authorized to view the project
        boolean isPublished = Boolean.TRUE.equals(project.getPublished());
        boolean isOwner = project.getCreatedBy().equals(username);  // Owner of the project (PM)
        boolean isDepartmentHead = "DepartmentHead".equals(username);  // Example check for Department Head (adjust as per your logic)

        // Only show project details if it's published or if the user is the owner
        if (!isPublished && !isOwner) {
            return "redirect:/ui/projects?error=Unauthorized+to+view+this+project";
        }


        // Get related data like assignments, applications, pending applications count, etc.
        List<Assignment> assignments = assignmentRepository.findAll().stream()
                .filter(a -> a.getProject().getId().equals(id))
                .collect(Collectors.toList());

        List<Application> applications = applicationRepository.findAll().stream()
                .filter(app -> app.getProject().getId().equals(id))
                .collect(Collectors.toList());

        long pendingApplicationsCount = applicationRepository.countByStatusAndProject_CreatedBy(
                ApplicationStatus.PENDING, username);
        model.addAttribute("pendingApplicationsCount", pendingApplicationsCount);

        // Get available employees for assignment
        List<Employee> availableEmployees = employeeRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getAvailable()))
                .collect(Collectors.toList());

        // Logic for external search and skill gap analysis
        boolean rpRequestedExternalSearch = "AWAITING_PM_DECISION".equals(project.getWorkflowStatus());

        String workflowStatus = project.getWorkflowStatus();
        boolean canTriggerExternalSearch = rpRequestedExternalSearch || "AWAITING_PM_DECISION".equals(workflowStatus);
        String externalSearchNotes = project.getExternalSearchNotes();

        // Skill gap analysis
        Map<String, Integer> skillGaps = new HashMap<>();
        boolean hasSkillGaps = false;

        if (project.getSkillRequirements() != null && !project.getSkillRequirements().isEmpty()) {
            for (ProjectSkillRequirement requirement : project.getSkillRequirements()) {
                String skill = requirement.getSkill();
                int requiredCount = requirement.getRequiredCount();

                // Count how many assigned employees have this skill
                long assignedWithSkill = assignments.stream()
                        .filter(a -> a.getEmployee().getSkills() != null)
                        .filter(a -> a.getEmployee().getSkills().contains(skill))
                        .count();

                int gap = requiredCount - (int) assignedWithSkill;
                if (gap > 0) {
                    skillGaps.put(skill, gap);
                    hasSkillGaps = true;
                }
            }
        }

        model.addAttribute("username", username);
        model.addAttribute("project", project);
        model.addAttribute("assignments", assignments);
        model.addAttribute("applications", applications);
        model.addAttribute("availableEmployees", availableEmployees);
        model.addAttribute("workflowStatus", workflowStatus);
        model.addAttribute("canTriggerExternalSearch", canTriggerExternalSearch);
        model.addAttribute("assignedCount", assignments.size());
        model.addAttribute("requiredCount", project.getTotalEmployeesRequired());
        model.addAttribute("hasSkillGaps", hasSkillGaps);
        model.addAttribute("skillGaps", skillGaps);
        model.addAttribute("externalSearchNotes", externalSearchNotes);

        // Show or hide buttons based on project status
        model.addAttribute("isPublished", isPublished);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isDepartmentHead", isDepartmentHead);

        // Mark PM notification as seen
        if (Boolean.FALSE.equals(project.getPmNotificationSeen())) {
            project.setPmNotificationSeen(true);
            projectRepository.save(project);
        }

        return "projects/view";
    }


    // ==================== EDIT PROJECT ====================
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Guest";

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Check ownership
        if (!project.getCreatedBy().equals(username)) {
            return "redirect:/ui/projects?error=Unauthorized+to+edit+this+project";
        }

        // Convert project to form
        CreateProjectForm form = new CreateProjectForm();
        form.setName(project.getName());
        form.setDescription(project.getDescription());
        form.setStartDate(project.getStartDate());
        form.setEndDate(project.getEndDate());
        form.setBudget(project.getBudget());
        form.setTotalEmployeesRequired(project.getTotalEmployeesRequired());
        form.setStatus(project.getStatus().toString());

        // Convert skill requirements to DTOs
        List<SkillRequirementDto> skillDtos = new ArrayList<>();
        for (ProjectSkillRequirement req : project.getSkillRequirements()) {
            SkillRequirementDto dto = new SkillRequirementDto();
            dto.setSkill(req.getSkill());
            dto.setRequiredCount(req.getRequiredCount());
            skillDtos.add(dto);
        }
        form.setSkillRequirements(skillDtos);


        model.addAttribute("username", username);
        model.addAttribute("projectForm", form);
        model.addAttribute("projectId", id);

        return "projects/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateProject(
            @PathVariable("id") Long id,
            @ModelAttribute CreateProjectForm form,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        String username = principal != null ? principal.getName() : "Guest";

        try {
            Project project = projectRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Check ownership
            if (!project.getCreatedBy().equals(username)) {
                return "redirect:/ui/projects?error=Unauthorized+to+edit+this+project";
            }

            // Update project fields
            project.setName(form.getName());
            project.setDescription(form.getDescription());
            project.setStartDate(form.getStartDate());
            project.setEndDate(form.getEndDate());
            project.setBudget(form.getBudget());
            project.setTotalEmployeesRequired(form.getTotalEmployeesRequired());

            // Clear and update skill requirements
            project.getSkillRequirements().forEach(skill -> skill.setProject(null));
            project.getSkillRequirements().clear();
            projectRepository.saveAndFlush(project);

            // Add new skills
            if (form.getSkillRequirements() != null) {
                for (SkillRequirementDto dto : form.getSkillRequirements()) {
                    if (dto != null && dto.getSkill() != null && !dto.getSkill().trim().isEmpty()) {
                        Integer count = dto.getRequiredCount();
                        if (count == null || count < 1) {
                            count = 1;
                        }

                        ProjectSkillRequirement requirement = new ProjectSkillRequirement();
                        requirement.setSkill(dto.getSkill().trim());
                        requirement.setRequiredCount(count);
                        requirement.setProject(project);

                        project.getSkillRequirements().add(requirement);
                    }
                }
            }
            project.setStatus(ProjectStatus.DRAFT);
            projectRepository.save(project);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Project '" + project.getName() + "' updated successfully!");
            return "redirect:/ui/projects/dashboard"; // REDIRECT TO DASHBOARD

        } catch (Exception e) {
            model.addAttribute("error", "Error updating project: " + e.getMessage());
            model.addAttribute("projectForm", form);
            model.addAttribute("projectId", id);
            model.addAttribute("username", username);
            return "projects/edit";
        }
    }

    // ==================== DELETE PROJECT ====================
    @PostMapping("/{id}/delete")
    public String deleteProject(
            @PathVariable("id") Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String username = principal != null ? principal.getName() : "Guest";

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Check ownership
        if (!project.getCreatedBy().equals(username)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Unauthorized to delete this project");
            return "redirect:/ui/projects/dashboard"; // REDIRECT TO DASHBOARD
        }

        // Check if project can be deleted
        if (project.getStatus() == ProjectStatus.IN_PROGRESS ||
                project.getStatus() == ProjectStatus.COMPLETED
                ||
                project.getStatus() == ProjectStatus.APPROVED) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot delete project that is " + project.getStatus());
            return "redirect:/ui/projects/dashboard"; // REDIRECT TO DASHBOARD
        }

        // Check if there are assignments
        long assignmentCount = assignmentRepository.findAll().stream()
                .filter(a -> a.getProject().getId().equals(id))
                .count();

        if (assignmentCount > 0) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cannot delete project with existing assignments");
            return "redirect:/ui/projects/dashboard"; // REDIRECT TO DASHBOARD
        }

        projectRepository.delete(project);

        redirectAttributes.addFlashAttribute("successMessage",
                "Project '" + project.getName() + "' deleted successfully!");
        return "redirect:/ui/projects/dashboard"; // REDIRECT TO DASHBOARD
    }

    // ==================== PUBLISH PROJECT ====================
    @PostMapping("/{id}/publish")
    public String publishProject(
            @PathVariable("id") Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String username = principal != null ? principal.getName() : "Guest";

        try {
            Project project = projectRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Check ownership
            if (!project.getCreatedBy().equals(username)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Unauthorized to publish this project");
                return "redirect:/ui/projects/" + id;
            }

            // ✅ FIX: Publish but keep PENDING for Department Head approval
            project.setPublished(true);
            project.setPublishedAt(LocalDateTime.now());
            project.setVisibleToAll(false);  // ✅ Not visible until approved
            project.setStatus(ProjectStatus.PENDING);  // ✅ PENDING, not APPROVED
            project.setWorkflowStatus("AWAITING_DEPARTMENT_HEAD_APPROVAL");  // ✅ Set workflow

            projectRepository.save(project);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Project '" + project.getName() + "' published successfully! " +
                            "Awaiting Department Head approval.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to publish project: " + e.getMessage());
        }

        return "redirect:/ui/projects/dashboard";
    }

    // ==================== UNPUBLISH PROJECT ====================

    @PostMapping("/{id}/unpublish")
    public String unpublishProject(
            @PathVariable("id") Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String username = principal != null ? principal.getName() : "Guest";

        try {
            Project project = projectRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Check ownership
            if (!project.getCreatedBy().equals(username)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Unauthorized to unpublish this project");
                return "redirect:/ui/projects/" + id;
            }

            // Check if there are pending applications
            long applicationCount = applicationRepository.findAll().stream()
                    .filter(app -> app.getProject().getId().equals(id))
                    .filter(app -> app.getStatus() == ApplicationStatus.PENDING)
                    .count();

            if (applicationCount > 0) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "⚠️ Cannot unpublish project with pending applications. " +
                                "Please review all applications first.");
                return "redirect:/ui/projects/" + id;
            }

            // ✅ FIX: Properly reset workflow status so it's removed from Department Head view
            project.setPublished(false);  // Unpublish it
            project.setPublishedAt(LocalDateTime.now());
            project.setVisibleToAll(false);  // Don't show it to employees
            project.setStatus(ProjectStatus.DRAFT);  // Mark as DRAFT

            // ✅ IMPORTANT: Set workflow status to something that WON'T appear in Department Head dashboard
            project.setWorkflowStatus("DRAFT");  // Changed from "PROJECT_CANCELLED" to "DRAFT"

            // ✅ Clear any approval-related fields
            project.setApprovedAt(null);
            project.setApprovedBy(null);
            project.setApprovalComments(null);

            projectRepository.save(project);

            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Project '" + project.getName() + "' unpublished successfully! " +
                            "No longer visible to employees or Department Head.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Failed to unpublish project: " + e.getMessage());
        }

        return "redirect:/ui/projects/dashboard";
    }


    // ==================== CANCEL PROJECT ====================
    @PostMapping("/{id}/cancel")
    public String cancelProject(
            @PathVariable("id") Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String username = principal != null ? principal.getName() : "Guest";

        try {
            Project project = projectRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Check ownership
            if (!project.getCreatedBy().equals(username)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Unauthorized to cancel this project");
                return "redirect:/ui/projects/dashboard";
            }

            // Check if project can be cancelled
            if (project.getStatus() == ProjectStatus.COMPLETED) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Cannot cancel a completed project");
                return "redirect:/ui/projects/" + id;
            }

            // Cancel the project
            project.setStatus(ProjectStatus.CANCELLED);
            project.setPublished(false);
            project.setVisibleToAll(false);
            projectRepository.save(project);

            // Free all assigned employees
            List<Assignment> assignments = assignmentRepository.findAll().stream()
                    .filter(a -> a.getProject().getId().equals(id))
                    .toList();

            for (Assignment assignment : assignments) {
                Employee employee = assignment.getEmployee();
                employee.setAvailable(true);
                employeeRepository.save(employee);
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Project '" + project.getName() + "' cancelled successfully! " +
                            "All employees freed.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Failed to cancel project: " + e.getMessage());
        }

        return "redirect:/ui/projects/dashboard";
    }

    // ==================== PROJECT APPLICATIONS ====================
    @GetMapping("/{id}/applications")
    public String viewApplications(
            @PathVariable("id") Long id,
            Model model,
            Principal principal) {

        String username = principal != null ? principal.getName() : "Guest";

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Check ownership
        if (!project.getCreatedBy().equals(username)) {
            return "redirect:/ui/projects?error=Unauthorized";
        }

        // Get applications for this project
        List<Application> applications = applicationRepository.findAll().stream()
                .filter(app -> app.getProject().getId().equals(id))
                .toList();

        model.addAttribute("username", username);
        model.addAttribute("project", project);
        model.addAttribute("applications", applications);

        return "projects/applications";
    }

    @PostMapping("/applications/{applicationId}/approve")
    public String approveApplication(
            @PathVariable("applicationId") Long applicationId,
            RedirectAttributes redirectAttributes) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        application.setStatus(ApplicationStatus.APPROVED);
        application.setReviewedAt(LocalDateTime.now());
        applicationRepository.save(application);

        // ✅ CREATE ASSIGNMENT (END-TO-END FLOW)
        Assignment assignment = new Assignment();
        assignment.setProject(application.getProject());
        assignment.setEmployee(application.getEmployee());
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        assignmentRepository.save(assignment);

        // ✅ Mark employee unavailable
        Employee employee = application.getEmployee();
        employee.setAvailable(false);
        employeeRepository.save(employee);

        redirectAttributes.addFlashAttribute("successMessage",
                "Application approved and employee assigned: " +
                        employee.getName());

        return "redirect:/ui/projects/" +
                application.getProject().getId() + "/applications";
    }


    @PostMapping("/applications/{applicationId}/reject")
    public String rejectApplication(
            @PathVariable("applicationId") Long applicationId,
            RedirectAttributes redirectAttributes) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        application.setStatus(ApplicationStatus.REJECTED);
        application.setReviewedAt(LocalDateTime.now());
        applicationRepository.save(application);

        redirectAttributes.addFlashAttribute("successMessage",
                "Application rejected for employee: " + application.getEmployee().getName());
        return "redirect:/ui/projects/" + application.getProject().getId() + "/applications";
    }

    // ==================== PROJECT ASSIGNMENTS ====================
    @GetMapping("/{id}/assignments")
    public String viewAssignments(
            @PathVariable("id") Long id,
            Model model,
            Principal principal) {

        String username = principal != null ? principal.getName() : "Guest";

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Check ownership
        if (!project.getCreatedBy().equals(username)) {
            return "redirect:/ui/projects?error=Unauthorized";
        }

        // Get assignments for this project
        List<Assignment> assignments = assignmentRepository.findAll().stream()
                .filter(a -> a.getProject().getId().equals(id))
                .toList();

        // Get all available employees
        List<Employee> availableEmployees = employeeRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getAvailable()))
                .collect(Collectors.toList());

        model.addAttribute("username", username);
        model.addAttribute("project", project);
        model.addAttribute("assignments", assignments);
        model.addAttribute("availableEmployees", availableEmployees);

        return "projects/assignments";
    }

    @PostMapping("/{id}/assignments/create")
    public String createAssignment(
            @PathVariable("id") Long id,
            @RequestParam Long employeeId,
            RedirectAttributes redirectAttributes) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Check if employee is already assigned
        boolean alreadyAssigned = assignmentRepository.findAll().stream()
                .anyMatch(a -> a.getProject().getId().equals(id) &&
                        a.getEmployee().getId().equals(employeeId));

        if (alreadyAssigned) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    employee.getName() + " is already assigned to this project");
            return "redirect:/ui/projects/" + id + "/assignments";
        }

        // Create new assignment
        Assignment assignment = new Assignment();
        assignment.setProject(project);
        assignment.setEmployee(employee);
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        assignmentRepository.save(assignment);

        // Mark employee as unavailable
        employee.setAvailable(false);
        employeeRepository.save(employee);

        redirectAttributes.addFlashAttribute("successMessage",
                employee.getName() + " assigned to project successfully!");
        return "redirect:/ui/projects/" + id + "/assignments";
    }

    @PostMapping("/assignments/{assignmentId}/remove")
    public String removeAssignment(
            @PathVariable("assignmentId") Long assignmentId,
            RedirectAttributes redirectAttributes) {

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        Long projectId = assignment.getProject().getId();
        Employee employee = assignment.getEmployee();

        // Remove assignment
        assignmentRepository.delete(assignment);

        // Mark employee as available
        employee.setAvailable(true);
        employeeRepository.save(employee);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                employee.getName() + " removed from project"
        );

        return "redirect:/ui/projects/" + projectId + "/assignments";
    }


    // ==================== TRIGGER EXTERNAL SEARCH ====================
    @PostMapping("/{id}/external-search")
    public String triggerExternalSearch(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Mark that external search is needed
        project.setExternalSearchNeeded(true);
        //project.setStatus(ProjectStatus.STAFFING);
        projectRepository.save(project);

        redirectAttributes.addFlashAttribute("successMessage",
                "Request for external search of candidate sent to department head " + project.getName() +
                        ". Waiting for approval");
        return "redirect:/ui/projects/" + id;
    }

    @PostMapping("/{id}/request-external-search")
    public String requestExternalSearch(
            @PathVariable("id") Long id,
            @RequestParam(required = false) String justification,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String username = principal != null ? principal.getName() : "Guest";


        try {
            Project project = projectRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            String processInstanceId = externalSearchService.triggerExternalSearch(id, username, justification);

            Notification dhNotification = new Notification();
            dhNotification.setUsername("head");  // Your single DH username
            dhNotification.setTitle("External Search Request");
            dhNotification.setMessage("Project Manager " + username +
                    " requested external search for project '" +
                    project.getName() + "'" +
                    (justification != null ? ". Reason: " + justification : ""));
            dhNotification.setProjectId(project.getId());
            dhNotification.setProjectName(project.getName());
            dhNotification.setCreatedAt(LocalDateTime.now());
            dhNotification.setIsRead(false);
            notificationRepository.save(dhNotification);

            redirectAttributes.addFlashAttribute("successMessage",
                    " External search request submitted successfully! " +
                            "Awaiting Department Head approval.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to request external search: " + e.getMessage());
        }


        return "redirect:/ui/projects/" + id;
    }

    @GetMapping("/{id}/external-search-status")
    public String viewExternalSearchStatus(
            @PathVariable("id") Long id,
            Model model,
            Principal principal) {

        String username = principal != null ? principal.getName() : "Guest";
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Check ownership
        if (!project.getCreatedBy().equals(username)) {
            return "redirect:/ui/projects?error=Unauthorized";
        }

        model.addAttribute("username", username);
        model.addAttribute("project", project);

        return "projects/external-search-status";
    }

    // ==================== PROJECT MANAGER DASHBOARD ====================
    @GetMapping("/dashboard")
    public String projectManagerDashboard(Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Guest";

        List<Project> myProjects = projectRepository.findByCreatedBy(username);

        // Calculate statistics
        long totalProjects = myProjects.size();
        long pendingProjects = myProjects.stream()
                .filter(p ->
                        p.getStatus() == ProjectStatus.PENDING &&
                                Boolean.TRUE.equals(p.getPublished())
                )
                .count();
        long activeProjects = myProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.IN_PROGRESS)
                .count();

        long totalCount = myProjects.size();
        long pendingCount = myProjects.stream().filter(p -> p.getStatus() == ProjectStatus.PENDING_APPROVAL).count();
        long approvedCount = myProjects.stream().filter(p -> p.getStatus() == ProjectStatus.APPROVED).count();
        long publishedCount = myProjects.stream().filter(p -> Boolean.TRUE.equals(p.getPublished())).count();
        long draftCount = myProjects.stream().filter(p -> p.getStatus() == ProjectStatus.DRAFT).count();
        long rejectCount = myProjects.stream().filter(p -> p.getStatus() == ProjectStatus.REJECTED).count();
        long staffingCount = myProjects.stream().filter(p -> p.getStatus() == ProjectStatus.STAFFING).count();

        // Get published projects for department head
        List<Project> publishedProjects = projectRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getPublished()))  // Only include published projects
                .filter(p -> p.getStatus() == ProjectStatus.PENDING)
                .filter(p -> "AWAITING_DEPARTMENT_HEAD_APPROVAL".equals(p.getWorkflowStatus()))
                .collect(Collectors.toList());


        long pendingApplicationsCount =
                applicationRepository.countByStatusAndProject_CreatedBy(
                        ApplicationStatus.PENDING,
                        username
                );

        model.addAttribute("pendingApplicationsCount", pendingApplicationsCount);



        // Recent projects (last 5)
        List<Project> recentProjects = myProjects.stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .limit(5)
                .toList();

        // Get notifications for the dashboard
        List<Notification> unreadNotifications  = notificationRepository
                .findByUsernameAndIsReadFalseOrderByCreatedAtDesc(username);
        long notificationCount = unreadNotifications.size();

        // Get notifications for this PM
        List<Notification> pmNotifications = notificationRepository
                .findByUsernameOrderByCreatedAtDesc(username);

        // Convert to map for template
        List<Map<String, Object>> notificationList = new ArrayList<>();
        for (Notification notification : pmNotifications) {
            Map<String, Object> notif = new HashMap<>();
            notif.put("id", notification.getId());
            notif.put("title", notification.getTitle() != null ? notification.getTitle() : "Notification");
            notif.put("message", notification.getMessage());
            notif.put("projectId", notification.getProjectId());
            notif.put("projectName", notification.getProjectName());
            notif.put("createdAt", notification.getCreatedAt());
            notif.put("isRead", notification.getIsRead());
            notificationList.add(notif);
        }

        model.addAttribute("username", username);
        model.addAttribute("totalProjects", totalProjects);
        model.addAttribute("pendingProjects", pendingProjects);
        model.addAttribute("activeProjects", activeProjects);
        model.addAttribute("publishedProjects", publishedProjects);
        model.addAttribute("pendingApplicationsCount", pendingApplicationsCount);
        model.addAttribute("recentProjects", recentProjects);
        model.addAttribute("projects", myProjects);


        model.addAttribute("totalCount", totalCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("draftCount", draftCount);
        model.addAttribute("rejectCount", rejectCount);
        model.addAttribute("staffingCount", staffingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("publishedCount", publishedCount);


        // Add notification attributes
        model.addAttribute("notifications", notificationList);
        model.addAttribute("notificationCount", notificationCount);
        model.addAttribute("pmNotifications", pmNotifications);
        model.addAttribute("unreadNotifications", unreadNotifications);
        model.addAttribute("notificationCount", unreadNotifications.size());

        return "projects/dashboard";
    }

    // Helper method to mark all project-related notifications as read
    private void markAllProjectNotificationsAsRead(Long projectId, String username) {
        try {
            // Mark project-specific notification flag
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project != null && Boolean.FALSE.equals(project.getPmNotificationSeen())) {
                project.setPmNotificationSeen(true);
                projectRepository.save(project);
            }

            // Mark all database notifications for this project and user as read
            List<Notification> unreadNotifications = notificationRepository
                    .findByUsernameAndIsReadFalseOrderByCreatedAtDesc(username);

            for (Notification notification : unreadNotifications) {
                if (projectId.equals(notification.getProjectId())) {
                    notification.setIsRead(true);
                }
            }
            notificationRepository.saveAll(unreadNotifications);

        } catch (Exception e) {
            // Log error but don't break the flow
            System.err.println("Error marking notifications as read: " + e.getMessage());
        }
    }
}