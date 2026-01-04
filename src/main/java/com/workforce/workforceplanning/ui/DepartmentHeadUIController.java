package com.workforce.workforceplanning.ui;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.repository.ProjectRepository;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.workforce.workforceplanning.model.ProjectStatus;
import java.security.Principal;
import java.util.*;

@Controller
@RequestMapping("/ui/department-head")
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

        // Get all pending approval tasks for Department Head
        List<Task> pendingTasks = taskService.createTaskQuery()
                .taskCandidateGroup("DepartmentHead")
                .active()
                .orderByTaskCreateTime()
                .desc()
                .list();

        // Get recently completed tasks (last 50)
        List<HistoricTaskInstance> recentTasks = historyService.createHistoricTaskInstanceQuery()
                .taskCandidateGroup("DepartmentHead")
                .finished()
                .orderByTaskCreateTime()
                .desc()
                .list()
                .stream()
                .limit(10)
                .toList();

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

        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("recentTasks", recentTasks);
        model.addAttribute("pendingCount", pendingTasks.size());
        model.addAttribute("totalApprovals", totalApprovals);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("approvalRate", approvalRate);

        return "department-head/dashboard";
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

    // ==================== APPROVE PROJECT REQUEST ====================
    @PostMapping("/tasks/{taskId}/approve")
    public String approveTask(
            @PathVariable String taskId,
            @RequestParam(required = false) String approvalNotes,
            @RequestParam(required = false, defaultValue = "false") Boolean urgent,
            @RequestParam(required = false) Integer priority,
            RedirectAttributes redirectAttributes,
            Principal principal) {

        try {
            String approver = principal != null ? principal.getName() : "DepartmentHead";

            // Get current variables to preserve them
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            Map<String, Object> currentVariables = runtimeService.getVariables(task.getProcessInstanceId());

            // === UPDATE PROJECT STATUS DIRECTLY IN CONTROLLER ===
            Long projectId = null;
            if (currentVariables.containsKey("projectId")) {
                projectId = ((Number) currentVariables.get("projectId")).longValue();
                Project project = projectRepository.findById(projectId).orElse(null);
                if (project != null) {
                    project.setStatus(ProjectStatus.APPROVED);
                    projectRepository.save(project);
                    System.out.println("✅ Project " + projectId + " approved directly in controller");
                }
            }
            // === END DIRECT UPDATE ===

            // Create approval variables
            Map<String, Object> variables = new HashMap<>(currentVariables);
            variables.put("approved", true);
            variables.put("approvalNotes", approvalNotes != null ? approvalNotes : "Approved by " + approver);
            variables.put("approvedBy", approver);
            variables.put("approvalTime", new Date());
            variables.put("urgent", urgent != null ? urgent : false);
            variables.put("priority", priority != null ? priority : 3);
            variables.put("approvedEmployeeIds", new ArrayList<>());

            // Complete the task - this triggers ProjectApprovalDelegate
            taskService.complete(taskId, variables);

            // Log for debugging
            System.out.println("✅ Project approved by Department Head: " + approver);
            System.out.println("   Task ID: " + taskId);
            System.out.println("   Process Instance: " + task.getProcessInstanceId());

            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Project request approved successfully. Resource Planner will now find suitable employees.");

        } catch (Exception e) {
            System.err.println("❌ Error approving project: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error approving project: " + e.getMessage());
        }

        return "redirect:/ui/department-head/dashboard";
    }

    // ==================== REJECT PROJECT REQUEST ====================
    @PostMapping("/tasks/{taskId}/reject")
    public String rejectTask(
            @PathVariable String taskId,
            @RequestParam(required = false) String rejectionReason,
            @RequestParam(required = false) String alternativeSuggestions,
            RedirectAttributes redirectAttributes,
            Principal principal) {

        try {
            String rejector = principal != null ? principal.getName() : "DepartmentHead";

            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", false);
            variables.put("rejectionReason", rejectionReason != null ? rejectionReason : "Rejected by " + rejector);
            variables.put("rejectedBy", rejector);
            variables.put("rejectionTime", new Date());
            variables.put("alternativeSuggestions", alternativeSuggestions);

            taskService.complete(taskId, variables);

            System.out.println("❌ Project rejected by Department Head: " + rejector);
            System.out.println("   Reason: " + rejectionReason);

            redirectAttributes.addFlashAttribute("successMessage",
                    "❌ Project request rejected successfully.");

        } catch (Exception e) {
            System.err.println("❌ Error rejecting project: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error rejecting project: " + e.getMessage());
        }

        return "redirect:/ui/department-head/dashboard";
    }

    // ==================== DEFER DECISION (REQUEST MORE INFO) ====================
    @PostMapping("/tasks/{taskId}/defer")
    public String deferTask(
            @PathVariable String taskId,
            @RequestParam String infoRequested,
            @RequestParam(required = false) String deadline,
            RedirectAttributes redirectAttributes,
            Principal principal) {

        try {
            String deferrer = principal != null ? principal.getName() : "DepartmentHead";

            // Instead of completing, we add a comment/note and reassign or leave pending
            Map<String, Object> variables = new HashMap<>();
            variables.put("infoRequested", infoRequested);
            variables.put("requestedBy", deferrer);
            variables.put("requestTime", new Date());
            variables.put("deadline", deadline);

            // Add these as local task variables (not process variables)
            taskService.setVariablesLocal(taskId, variables);

            // Add a comment to the task
            taskService.addComment(taskId, null,
                    "Decision deferred by " + deferrer + ". Info requested: " + infoRequested);

            System.out.println("⏸️ Decision deferred by " + deferrer);
            System.out.println("   Info requested: " + infoRequested);

            redirectAttributes.addFlashAttribute("successMessage",
                    "⏸️ Decision deferred. Additional information has been requested.");

        } catch (Exception e) {
            System.err.println("❌ Error deferring decision: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error deferring decision: " + e.getMessage());
        }

        return "redirect:/ui/department-head/dashboard";
    }

    // ==================== VIEW APPROVAL HISTORY ====================
    @GetMapping("/history")
    public String approvalHistory(Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Guest";

        // Get completed tasks from history
        List<HistoricTaskInstance> completedTasks = historyService.createHistoricTaskInstanceQuery()
                .taskCandidateGroup("DepartmentHead")
                .finished()
                .orderByTaskCreateTime()
                .desc()
                .list()
                .stream()
                .limit(50)
                .toList();

        // Get statistics
        long totalApprovals = historyService.createHistoricTaskInstanceQuery()
                .taskCandidateGroup("DepartmentHead")
                .finished()
                .count();

        // Count approved vs rejected (simplified - check variables in real implementation)
        long approvedCount = completedTasks.stream()
                .filter(task -> {
                    // In real app, you'd check process variables
                    // For now, just estimate
                    return task.getDeleteReason() == null ||
                            !task.getDeleteReason().toLowerCase().contains("reject");
                })
                .count();

        long rejectedCount = totalApprovals - approvedCount;

        model.addAttribute("username", username);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("totalApprovals", totalApprovals);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("rejectedCount", rejectedCount);

        return "department-head/history";
    }

    // ==================== BULK ACTIONS ====================
    @PostMapping("/bulk-approve")
    public String bulkApprove(
            @RequestParam List<String> taskIds,
            @RequestParam(required = false) String bulkNotes,
            RedirectAttributes redirectAttributes,
            Principal principal) {

        try {
            String approver = principal != null ? principal.getName() : "DepartmentHead";
            int successCount = 0;
            int errorCount = 0;

            for (String taskId : taskIds) {
                try {
                    Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
                    if (task != null) {
                        Map<String, Object> currentVariables = runtimeService.getVariables(task.getProcessInstanceId());
                        Map<String, Object> variables = new HashMap<>(currentVariables);
                        variables.put("approved", true);
                        variables.put("approvalNotes", "Bulk approved by " + approver + ": " + bulkNotes);
                        variables.put("approvedBy", approver);
                        variables.put("approvedEmployeeIds", new ArrayList<>());

                        taskService.complete(taskId, variables);
                        successCount++;
                    } else {
                        errorCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    System.err.println("Error approving task " + taskId + ": " + e.getMessage());
                }
            }

            String message = "✅ Bulk approval completed: " + successCount + " approved, " + errorCount + " failed.";
            redirectAttributes.addFlashAttribute("successMessage", message);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error in bulk approval: " + e.getMessage());
        }

        return "redirect:/ui/department-head/dashboard";
    }
}