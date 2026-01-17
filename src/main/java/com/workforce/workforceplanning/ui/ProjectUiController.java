package com.workforce.workforceplanning.ui;

import com.workforce.workforceplanning.dto.CreateProjectForm;
import com.workforce.workforceplanning.dto.SkillRequirementDto;
import com.workforce.workforceplanning.model.*;
import com.workforce.workforceplanning.repository.*;
import com.workforce.workforceplanning.service.ExternalSearchService;
import com.workforce.workforceplanning.service.SkillGapAnalysisService;
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

@Controller
@RequestMapping("/ui/projects")
public class ProjectUiController {

    private final ProjectRepository projectRepository;
    private final AssignmentRepository assignmentRepository;
    private final ApplicationRepository applicationRepository;
    private final EmployeeRepository employeeRepository;
    private final ExternalSearchService externalSearchService;
    private final SkillGapAnalysisService skillGapAnalysisService;

    public ProjectUiController(
            ProjectRepository projectRepository,
            AssignmentRepository assignmentRepository,
            ApplicationRepository applicationRepository,
            ExternalSearchService externalSearchService,
            SkillGapAnalysisService skillGapAnalysisService,
            EmployeeRepository employeeRepository) {
        this.projectRepository = projectRepository;
        this.assignmentRepository = assignmentRepository;
        this.applicationRepository = applicationRepository;
        this.employeeRepository = employeeRepository;
        this.externalSearchService = externalSearchService;
        this.skillGapAnalysisService = skillGapAnalysisService;
    }

    // ==================== PROJECT LIST ====================
    @GetMapping
    public String myProjects(Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Guest";
        model.addAttribute("username", username);

        List<Project> projects = projectRepository.findByCreatedBy(username);

        // Calculate statistics
        long totalCount = projects.size();
        long pendingCount = projects.stream().filter(p -> p.getStatus() == ProjectStatus.PENDING).count();
        long approvedCount = projects.stream().filter(p -> p.getStatus() == ProjectStatus.APPROVED).count();
        long publishedCount = projects.stream().filter(p -> Boolean.TRUE.equals(p.getPublished())).count();

        // ✅ SIMPLIFIED NOTIFICATIONS - NO MORE ERRORS
        List<Map<String, Object>> notifications = new ArrayList<>();

        for (Project project : projects) {
            // Notification 1: Resource Planner found skill gaps
            if (Boolean.TRUE.equals(project.getExternalSearchNeeded())
                    && "AWAITING_PM_DECISION".equals(project.getWorkflowStatus())
                    && Boolean.FALSE.equals(project.getPmNotificationSeen())) {


                Map<String, Object> notif = new HashMap<>();
                notif.put("id", project.getId()); // ✅ Always set ID
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

                notif.put("createdAt", project.getExternalSearchRequestedAt() != null
                        ? project.getExternalSearchRequestedAt()
                        : LocalDateTime.now());

                notifications.add(notif);
            }
        }

        model.addAttribute("projects", projects);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("publishedCount", publishedCount);
        model.addAttribute("notifications", notifications); // ✅ REMOVED DUPLICATE
        model.addAttribute("notificationCount", notifications.size());

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
            project.setStatus(ProjectStatus.PENDING);

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
    public String viewProject(@PathVariable Long id, Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Guest";

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // ✅ SIMPLE FIX: Check ownership or if published
        // Department Head and Resource Planner can view published projects
        if (!project.getCreatedBy().equals(username) && !Boolean.TRUE.equals(project.getPublished())) {
            return "redirect:/ui/projects?error=Unauthorized+to+view+this+project";
        }


        // Get related data
        List<Assignment> assignments = assignmentRepository.findAll().stream()
                .filter(a -> a.getProject().getId().equals(id))
                .toList();

        List<Application> applications = applicationRepository.findAll().stream()
                .filter(app -> app.getProject().getId().equals(id))
                .toList();

        // Get all available employees
        List<Employee> availableEmployees = employeeRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getAvailable()))
                .collect(Collectors.toList());

        // Check if Resource Planner has requested external search
        boolean rpRequestedExternalSearch = Boolean.TRUE.equals(project.getExternalSearchNeeded()) &&
                "AWAITING_PM_DECISION".equals(project.getWorkflowStatus());

        // Check current workflow status
        String workflowStatus = project.getWorkflowStatus();
        boolean canTriggerExternalSearch = rpRequestedExternalSearch ||
                ("AWAITING_PM_DECISION".equals(workflowStatus));

        // Get external search notes from Resource Planner
        String externalSearchNotes = project.getExternalSearchNotes();

        // ============ ADD SKILL GAP ANALYSIS ============
        Map<String, Integer> skillGaps = new HashMap<>();
        boolean hasSkillGaps = false;

        // Analyze skill gaps between project requirements and assigned employees
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

        // Add skill gap attributes
        model.addAttribute("hasSkillGaps", hasSkillGaps);
        model.addAttribute("skillGaps", skillGaps);
        model.addAttribute("externalSearchNotes", externalSearchNotes);

        // ✅ Mark PM notification as seen
        if (Boolean.FALSE.equals(project.getPmNotificationSeen())) {
            project.setPmNotificationSeen(true);
            projectRepository.save(project);
        }


        return "projects/view";
    }

    // ==================== EDIT PROJECT ====================
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, Principal principal) {
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
            @PathVariable Long id,
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

            projectRepository.save(project);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Project '" + project.getName() + "' updated successfully!");
            return "redirect:/ui/projects/dashboard"; // ✅ REDIRECT TO DASHBOARD

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
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String username = principal != null ? principal.getName() : "Guest";

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Check ownership
        if (!project.getCreatedBy().equals(username)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Unauthorized to delete this project");
            return "redirect:/ui/projects/dashboard"; // ✅ REDIRECT TO DASHBOARD
        }

        // Check if project can be deleted
        if (project.getStatus() == ProjectStatus.IN_PROGRESS ||
                project.getStatus() == ProjectStatus.COMPLETED) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot delete project that is " + project.getStatus());
            return "redirect:/ui/projects/dashboard"; // ✅ REDIRECT TO DASHBOARD
        }

        // Check if there are assignments
        long assignmentCount = assignmentRepository.findAll().stream()
                .filter(a -> a.getProject().getId().equals(id))
                .count();

        if (assignmentCount > 0) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot delete project with existing assignments");
            return "redirect:/ui/projects/dashboard"; // ✅ REDIRECT TO DASHBOARD
        }

        projectRepository.delete(project);

        redirectAttributes.addFlashAttribute("successMessage",
                "Project '" + project.getName() + "' deleted successfully!");
        return "redirect:/ui/projects/dashboard"; // ✅ REDIRECT TO DASHBOARD
    }

    // ==================== PUBLISH PROJECT ====================
    @PostMapping("/{id}/publish")
    public String publishProject(
            @PathVariable Long id,
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
                    "✅ Project '" + project.getName() + "' published successfully! " +
                            "Awaiting Department Head approval.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Failed to publish project: " + e.getMessage());
        }

        return "redirect:/ui/projects/dashboard";
    }

    // ==================== UNPUBLISH PROJECT ====================
    @PostMapping("/{id}/unpublish")
    public String unpublishProject(
            @PathVariable Long id,
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

            // Unpublish the project
            project.setPublished(false);
            project.setVisibleToAll(false);
            project.setStatus(ProjectStatus.PENDING);
            projectRepository.save(project);

            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Project '" + project.getName() + "' unpublished successfully! " +
                            "No longer visible to employees.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Failed to unpublish project: " + e.getMessage());
        }

        return "redirect:/ui/projects/dashboard";
    }

    // ==================== CANCEL PROJECT ====================
    @PostMapping("/{id}/cancel")
    public String cancelProject(
            @PathVariable Long id,
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
            @PathVariable Long id,
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
            @PathVariable Long applicationId,
            RedirectAttributes redirectAttributes) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        application.setStatus(ApplicationStatus.APPROVED);
        application.setReviewedAt(LocalDateTime.now());
        applicationRepository.save(application);

        redirectAttributes.addFlashAttribute("successMessage",
                "Application approved for employee: " + application.getEmployee().getName());
        return "redirect:/ui/projects/" + application.getProject().getId() + "/applications";
    }

    @PostMapping("/applications/{applicationId}/reject")
    public String rejectApplication(
            @PathVariable Long applicationId,
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
            @PathVariable Long id,
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
            @PathVariable Long id,
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
            @PathVariable Long assignmentId,
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

        redirectAttributes.addFlashAttribute("successMessage",
                employee.getName() + " removed from project");
        return "redirect:/ui/projects/" + projectId + "/assignments";
    }

    // ==================== TRIGGER EXTERNAL SEARCH ====================
    @PostMapping("/{id}/external-search")
    public String triggerExternalSearch(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Mark that external search is needed
        project.setExternalSearchNeeded(true);
        projectRepository.save(project);

        redirectAttributes.addFlashAttribute("successMessage",
                "External search triggered for project: " + project.getName() +
                        ". The recruitment team has been notified.");
        return "redirect:/ui/projects/" + id;
    }

    @PostMapping("/{id}/request-external-search")
    public String requestExternalSearch(
            @PathVariable Long id,
            @RequestParam(required = false) String justification,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String username = principal != null ? principal.getName() : "Guest";

        try {
            String processInstanceId = externalSearchService.triggerExternalSearch(id, username, justification);

            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ External search request submitted successfully! " +
                            "Awaiting Department Head approval.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Failed to request external search: " + e.getMessage());
        }

        return "redirect:/ui/projects/" + id;
    }

    @GetMapping("/{id}/external-search-status")
    public String viewExternalSearchStatus(
            @PathVariable Long id,
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
                .filter(p -> p.getStatus() == ProjectStatus.PENDING)
                .count();
        long activeProjects = myProjects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.IN_PROGRESS)
                .count();
        long publishedProjects = myProjects.stream()
                .filter(p -> Boolean.TRUE.equals(p.getPublished()))
                .count();

        // Get pending applications count
        long pendingApplications = applicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.PENDING)
                .filter(app -> app.getProject().getCreatedBy().equals(username))
                .count();

        // Recent projects (last 5)
        List<Project> recentProjects = myProjects.stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .limit(5)
                .toList();

        model.addAttribute("username", username);
        model.addAttribute("totalProjects", totalProjects);
        model.addAttribute("pendingProjects", pendingProjects);
        model.addAttribute("activeProjects", activeProjects);
        model.addAttribute("publishedProjects", publishedProjects);
        model.addAttribute("pendingApplications", pendingApplications);
        model.addAttribute("recentProjects", recentProjects);
        model.addAttribute("projects", myProjects);

        return "projects/dashboard";
    }
}