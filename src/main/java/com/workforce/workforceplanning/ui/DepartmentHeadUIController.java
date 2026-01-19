package com.workforce.workforceplanning.ui;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.model.ProjectStatus;
import com.workforce.workforceplanning.repository.ProjectRepository;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ui/department-head")
@Transactional
public class DepartmentHeadUIController {

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final ProjectRepository projectRepository;

    public DepartmentHeadUIController(TaskService taskService,
                                      RuntimeService runtimeService,
                                      HistoryService historyService,
                                      ProjectRepository projectRepository) {
        this.taskService = taskService;
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.projectRepository = projectRepository;
    }

    // ==================== DASHBOARD ====================
    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Guest";
        model.addAttribute("username", username);

        // ==================== WORKFLOW TASKS ====================

        // Get all pending approval tasks for Department Head
        List<Task> pendingTasks = taskService.createTaskQuery()
                .taskCandidateGroup("DepartmentHead")
                .active()
                .orderByTaskCreateTime()
                .desc()
                .list();

        Map<String, String> taskProjectNames = new HashMap<>();
        for (Task t : pendingTasks) {
            try {
                Map<String, Object> vars = runtimeService.getVariables(t.getProcessInstanceId());
                Object pidObj = vars.get("projectId");

                if (pidObj != null) {
                    Long pid = ((Number) pidObj).longValue();
                    projectRepository.findById(pid).ifPresent(p ->
                            taskProjectNames.put(t.getId(), p.getName())
                    );
                }
            } catch (Exception ignored) {
            }
        }

        // Get external search approval tasks
        List<Task> pendingExternalSearchTasks = taskService.createTaskQuery()
                .taskCandidateGroup("DepartmentHead")
                .taskName("Approve External Search")
                .active()
                .orderByTaskCreateTime()
                .desc()
                .list();

        // ==================== PUBLISHED PROJECTS AWAITING APPROVAL ====================

        // Get published projects for department head
        List<Project> publishedProjects = projectRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getPublished()))  // Only include published projects
                .filter(p -> p.getStatus() == ProjectStatus.PENDING)  // Only projects that are pending approval
                .filter(p -> "AWAITING_DEPARTMENT_HEAD_APPROVAL".equals(p.getWorkflowStatus())) // Awaiting approval
                .collect(Collectors.toList());


        // ==================== RECENT APPROVALS ====================

        // Get recently completed tasks (last 10)
        List<HistoricTaskInstance> recentTasks = historyService.createHistoricTaskInstanceQuery()
                .taskCandidateGroup("DepartmentHead")
                .finished()
                .orderByTaskCreateTime()
                .desc()
                .list()
                .stream()
                .limit(10)
                .toList();

        // ==================== STATISTICS ====================

        // Calculate approval rate
        long totalApprovals = historyService.createHistoricTaskInstanceQuery()
                .taskCandidateGroup("DepartmentHead")
                .finished()
                .count();

        long approvedCount = recentTasks.stream()
                .filter(task -> task.getDeleteReason() == null)
                .count();

        double approvalRate = 0.0;
        if (totalApprovals > 0) {
            approvalRate = (approvedCount * 100.0) / totalApprovals;
        }

        // ==================== ADD TO MODEL ====================

        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("recentTasks", recentTasks);
        model.addAttribute("taskProjectNames", taskProjectNames);

        // Counts
        int totalPendingCount = pendingTasks.size() + publishedProjects.size();
        model.addAttribute("pendingCount", totalPendingCount);
        model.addAttribute("pendingExternalSearchCount", pendingExternalSearchTasks.size());
        model.addAttribute("publishedProjectsCount", publishedProjects.size());

        // Projects
        model.addAttribute("publishedProjects", publishedProjects);
        model.addAttribute("pendingExternalSearchTasks", pendingExternalSearchTasks);

        // Statistics
        model.addAttribute("totalApprovals", totalApprovals);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("approvalRate", approvalRate);

        return "department-head/dashboard";
    }

    // ==================== VIEW PROJECT DETAILS (Department Head) ====================
    @GetMapping("/projects/{projectId}")
    public String viewProject(@PathVariable Long projectId, Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Guest";

        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Department Head can view published projects awaiting approval
            if (!Boolean.TRUE.equals(project.getPublished()) ||
                    !"AWAITING_DEPARTMENT_HEAD_APPROVAL".equals(project.getWorkflowStatus()) ||
                    "DRAFT".equals(project.getWorkflowStatus())) {  // ✅ NEW CHECK
                return "redirect:/ui/department-head/dashboard?error=Project+not+accessible";
            }
            model.addAttribute("project", project);
            model.addAttribute("username", username);

            return "department-head/project-detail";

        } catch (Exception e) {
            return "redirect:/ui/department-head/dashboard?error=" + e.getMessage();
        }
    }

    // ==================== APPROVE PUBLISHED PROJECT ====================
    @PostMapping("/projects/{projectId}/approve")
    public String approvePublishedProject(
            @PathVariable("projectId") Long projectId,
            @RequestParam(required = false) String approvalNotes,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String username = principal != null ? principal.getName() : "Guest";

        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Validate project state
            if (!"AWAITING_DEPARTMENT_HEAD_APPROVAL".equals(project.getWorkflowStatus())) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "❌ Project is not awaiting approval");
                return "redirect:/ui/department-head/dashboard";
            }

            // Approve the project
            project.setStatus(ProjectStatus.APPROVED);
            project.setWorkflowStatus("DEPARTMENT_HEAD_APPROVED");
            project.setApprovedAt(LocalDateTime.now());
            project.setApprovedBy(username);

            if (approvalNotes != null && !approvalNotes.isEmpty()) {
                project.setApprovalComments(approvalNotes);
            }

            projectRepository.save(project);

            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Project '" + project.getName() + "' approved successfully! Now available for staffing.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error approving project: " + e.getMessage());
        }

        return "redirect:/ui/department-head/dashboard";
    }

    // ==================== REJECT PUBLISHED PROJECT ====================
    @PostMapping("/projects/{projectId}/reject")
    public String rejectPublishedProject(
            @PathVariable Long projectId,
            @RequestParam(required = false) String rejectionReason,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String username = principal != null ? principal.getName() : "Guest";

        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Validate project state
            if (!"AWAITING_DEPARTMENT_HEAD_APPROVAL".equals(project.getWorkflowStatus())) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "❌ Project is not awaiting approval");
                return "redirect:/ui/department-head/dashboard";
            }

            // Reject the project
            project.setStatus(ProjectStatus.REJECTED);
            project.setWorkflowStatus("DEPARTMENT_HEAD_REJECTED");
            project.setPublished(false);
            project.setVisibleToAll(false);

            if (rejectionReason != null && !rejectionReason.isEmpty()) {
                project.setApprovalComments(rejectionReason);
            }

            projectRepository.save(project);

            redirectAttributes.addFlashAttribute("successMessage",
                    "❌ Project '" + project.getName() + "' rejected and unpublished.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error rejecting project: " + e.getMessage());
        }

        return "redirect:/ui/department-head/dashboard";
    }

    // ==================== VIEW TASK DETAILS ====================
    @GetMapping("/tasks/{taskId}")
    public String viewTask(@PathVariable String taskId, Model model, Principal principal) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            return "redirect:/ui/department-head/dashboard?error=Task+not+found";
        }

        // Get workflow variables
        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());

        // Get project details for review
        Long projectId = null;
        Project project = null;

        if (variables.containsKey("projectId")) {
            projectId = ((Number) variables.get("projectId")).longValue();
            project = projectRepository.findById(projectId).orElse(null);
        }

        model.addAttribute("task", task);
        model.addAttribute("variables", variables);
        model.addAttribute("project", project);
        model.addAttribute("projectId", projectId);
        model.addAttribute("username", principal != null ? principal.getName() : "Guest");

        return "department-head/task-detail";
    }

    // ==================== APPROVE WORKFLOW TASK ====================
    @PostMapping("/tasks/{taskId}/approve")
    public String approveTask(
            @PathVariable String taskId,
            @RequestParam(required = false) String approvalNotes,
            RedirectAttributes redirectAttributes,
            Principal principal) {

        try {
            String approver = principal != null ? principal.getName() : "DepartmentHead";

            // Get the task
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Task not found");
                return "redirect:/ui/department-head/dashboard";
            }

            String processInstanceId = task.getProcessInstanceId();

            // Set approval variables - The delegate will handle status update
            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", true);
            variables.put("approvalNotes", approvalNotes != null ? approvalNotes : "Approved by " + approver);
            variables.put("approvedBy", approver);
            variables.put("approvalTime", new Date());

            // Complete the task - This will trigger ProjectApprovalDelegate
            taskService.complete(taskId, variables);

            System.out.println("✅ Approval task completed. Delegate will update project status.");
            System.out.println("   Task ID: " + taskId);
            System.out.println("   Process Instance: " + processInstanceId);

            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Project request approved successfully!");

        } catch (Exception e) {
            System.err.println("❌ Error approving project: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error approving project: " + e.getMessage());
        }

        return "redirect:/ui/department-head/dashboard";
    }

    // ==================== REJECT WORKFLOW TASK ====================
    @PostMapping("/tasks/{taskId}/reject")
    public String rejectTask(
            @PathVariable String taskId,
            @RequestParam(required = false) String rejectionReason,
            RedirectAttributes redirectAttributes,
            Principal principal) {

        try {
            String rejector = principal != null ? principal.getName() : "DepartmentHead";

            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Task not found");
                return "redirect:/ui/department-head/dashboard";
            }

            // Set rejection variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", false);
            variables.put("rejectionReason", rejectionReason != null ? rejectionReason : "Rejected by " + rejector);
            variables.put("rejectedBy", rejector);
            variables.put("rejectionTime", new Date());

            // Complete the task
            taskService.complete(taskId, variables);

            redirectAttributes.addFlashAttribute("successMessage",
                    "❌ Project request rejected.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error rejecting project: " + e.getMessage());
        }

        return "redirect:/ui/department-head/dashboard";
    }

    // ==================== BULK APPROVE ====================
    @PostMapping("/bulk-approve")
    public String bulkApprove(
            @RequestParam(required = false) List<String> taskIds,
            @RequestParam(required = false) String bulkNotes,
            RedirectAttributes redirectAttributes,
            Principal principal) {

        if (taskIds == null || taskIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "⚠️ No tasks selected for approval");
            return "redirect:/ui/department-head/dashboard";
        }

        try {
            String approver = principal != null ? principal.getName() : "DepartmentHead";
            int successCount = 0;
            int failCount = 0;

            for (String taskId : taskIds) {
                try {
                    Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
                    if (task != null) {
                        Map<String, Object> variables = new HashMap<>();
                        variables.put("approved", true);
                        variables.put("approvalNotes", bulkNotes != null ? bulkNotes : "Bulk approved by " + approver);
                        variables.put("approvedBy", approver);
                        variables.put("approvalTime", new Date());

                        taskService.complete(taskId, variables);
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    System.err.println("Error approving task " + taskId + ": " + e.getMessage());
                    failCount++;
                }
            }

            if (successCount > 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "✅ Successfully approved " + successCount + " project(s)" +
                                (failCount > 0 ? " (" + failCount + " failed)" : ""));
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "❌ Failed to approve any projects");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error during bulk approval: " + e.getMessage());
        }

        return "redirect:/ui/department-head/dashboard";
    }

    // ==================== VIEW APPROVAL HISTORY ====================
    @GetMapping("/history")
    public String viewHistory(Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Guest";
        model.addAttribute("username", username);

        // Get completed workflow tasks
        List<HistoricTaskInstance> completedTasks = historyService.createHistoricTaskInstanceQuery()
                .taskCandidateGroup("DepartmentHead")
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .list();

        // Build approval history with proper structure
        List<Map<String, Object>> approvalHistory = new ArrayList<>();

        for (HistoricTaskInstance task : completedTasks) {
            try {
                // Get project details from workflow variables
                Map<String, Object> vars = null;
                try {
                    vars = task.getProcessVariables();
                } catch (Exception e) {
                    // If process variables not available, skip this task
                    continue;
                }

                // ✅ FIX: Make vars final for lambda
                final Map<String, Object> finalVars = vars;

                if (finalVars != null && finalVars.containsKey("projectId")) {
                    Long projectId = ((Number) finalVars.get("projectId")).longValue();

                    projectRepository.findById(projectId).ifPresent(project -> {
                        Map<String, Object> notification = new HashMap<>();

                        // ✅ ALWAYS SET projectId
                        notification.put("projectId", projectId);
                        notification.put("projectName", project.getName());
                        notification.put("taskName", task.getName());
                        notification.put("timestamp", task.getEndTime());

                        // ✅ FIX: Create decision inside lambda
                        String decision = "COMPLETED";
                        if (finalVars.containsKey("approved")) {
                            Boolean approved = (Boolean) finalVars.get("approved");
                            decision = Boolean.TRUE.equals(approved) ? "APPROVED" : "REJECTED";
                        }
                        notification.put("decision", decision);

                        // ✅ FIX: Create comments inside lambda
                        String comments = "";
                        if (finalVars.containsKey("approvalNotes")) {
                            comments = (String) finalVars.get("approvalNotes");
                        } else if (finalVars.containsKey("rejectionReason")) {
                            comments = (String) finalVars.get("rejectionReason");
                        }
                        notification.put("comments", comments);

                        approvalHistory.add(notification);
                    });
                }
            } catch (Exception e) {
                // Log but continue processing other tasks
                System.err.println("Error processing task " + task.getId() + ": " + e.getMessage());
            }
        }

        model.addAttribute("approvalHistory", approvalHistory);
        return "department-head/history";
    }

    // ==================== EXTERNAL SEARCH REQUESTS ====================
    @GetMapping("/external-search-requests")
    public String viewExternalSearchRequests(Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Guest";
        model.addAttribute("username", username);

        // Get all pending external search approval tasks
        List<Task> pendingExternalSearchTasks = taskService.createTaskQuery()
                .taskCandidateGroup("DepartmentHead")
                .taskName("Approve External Search")
                .active()
                .orderByTaskCreateTime()
                .desc()
                .list();

        // Group tasks with project info
        List<Map<String, Object>> externalSearchRequests = new ArrayList<>();
        for (Task task : pendingExternalSearchTasks) {
            Map<String, Object> processVariables = runtimeService.getVariables(task.getProcessInstanceId());
            Map<String, Object> request = new HashMap<>();

            request.put("task", task);
            request.put("variables", processVariables);

            // Get project details
            if (processVariables.containsKey("projectId")) {
                Long projectId = ((Number) processVariables.get("projectId")).longValue();
                Project project = projectRepository.findById(projectId).orElse(null);
                request.put("project", project);
            }

            externalSearchRequests.add(request);
        }

        model.addAttribute("externalSearchRequests", externalSearchRequests);
        model.addAttribute("pendingExternalSearchCount", pendingExternalSearchTasks.size());

        return "department-head/external-search-requests";
    }

    // ==================== APPROVE EXTERNAL SEARCH ====================
    @PostMapping("/external-search/tasks/{taskId}/approve")
    public String approveExternalSearchTask(
            @PathVariable String taskId,
            @RequestParam(required = false) String approvalNotes,
            RedirectAttributes redirectAttributes,
            Principal principal) {

        try {
            String approver = principal != null ? principal.getName() : "DepartmentHead";

            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Task not found");
                return "redirect:/ui/department-head/external-search-requests";
            }

            // Set approval variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", true);
            variables.put("externalSearchApproved", true);
            variables.put("approvalNotes", approvalNotes != null ? approvalNotes : "Approved by " + approver);
            variables.put("approvedBy", approver);

            // Complete the task
            taskService.complete(taskId, variables);

            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ External search approved successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error approving external search: " + e.getMessage());
        }

        return "redirect:/ui/department-head/external-search-requests";
    }

    // ==================== REJECT EXTERNAL SEARCH ====================
    @PostMapping("/external-search/tasks/{taskId}/reject")
    public String rejectExternalSearchTask(
            @PathVariable String taskId,
            @RequestParam(required = false) String rejectionReason,
            RedirectAttributes redirectAttributes,
            Principal principal) {

        try {
            String rejector = principal != null ? principal.getName() : "DepartmentHead";

            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Task not found");
                return "redirect:/ui/department-head/external-search-requests";
            }

            // Set rejection variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", false);
            variables.put("externalSearchApproved", false);
            variables.put("rejectionReason", rejectionReason != null ? rejectionReason : "Rejected by " + rejector);
            variables.put("rejectedBy", rejector);

            // Complete the task
            taskService.complete(taskId, variables);

            redirectAttributes.addFlashAttribute("successMessage",
                    "❌ External search rejected.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error rejecting external search: " + e.getMessage());
        }

        return "redirect:/ui/department-head/external-search-requests";
    }
}