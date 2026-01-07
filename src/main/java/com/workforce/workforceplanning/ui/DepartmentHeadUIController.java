package com.workforce.workforceplanning.ui;

import com.workforce.workforceplanning.model.Project;
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
import com.workforce.workforceplanning.model.ProjectStatus;
import java.security.Principal;
import java.util.*;

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

        // Get all pending approval tasks for Department Head
        List<Task> pendingTasks = taskService.createTaskQuery()
                .taskCandidateGroup("DepartmentHead")
                .active()
                .orderByTaskCreateTime()
                .desc()
                .list();

        // Get external search approval tasks
        List<Task> pendingExternalSearchTasks = taskService.createTaskQuery()
                .taskCandidateGroup("DepartmentHead")
                .taskName("Approve External Search")
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
        model.addAttribute("pendingExternalSearchCount", pendingExternalSearchTasks.size());

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
                    "✅ Project request approved successfully. Workflow will update status.");

        } catch (Exception e) {
            System.err.println("❌ Error approving project: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error approving project: " + e.getMessage());
        }

        return "redirect:/ui/department-head/dashboard";
    }

    // In DepartmentHeadUIController - REJECT method simplified
    @PostMapping("/tasks/{taskId}/reject")
    public String rejectTask(
            @PathVariable String taskId,
            @RequestParam(required = false) String rejectionReason,
            RedirectAttributes redirectAttributes,
            Principal principal) {

        try {
            String rejector = principal != null ? principal.getName() : "DepartmentHead";

            // Get the task
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Task not found");
                return "redirect:/ui/department-head/dashboard";
            }

            String processInstanceId = task.getProcessInstanceId();

            // Set rejection variables - The delegate will handle status update
            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", false);
            variables.put("rejectionReason", rejectionReason != null ? rejectionReason : "Rejected by " + rejector);
            variables.put("rejectedBy", rejector);
            variables.put("rejectionTime", new Date());

            // Complete the task - This will trigger ProjectApprovalDelegate
            taskService.complete(taskId, variables);

            System.out.println("❌ Rejection task completed. Delegate will update project status.");
            System.out.println("   Task ID: " + taskId);
            System.out.println("   Process Instance: " + processInstanceId);
            System.out.println("   Reason: " + rejectionReason);

            redirectAttributes.addFlashAttribute("successMessage",
                    "❌ Project request rejected successfully.");

        } catch (Exception e) {
            System.err.println("❌ Error rejecting project: " + e.getMessage());
            e.printStackTrace();
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

            // Get the task
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Task not found");
                return "redirect:/ui/department-head/dashboard";
            }

            String processInstanceId = task.getProcessInstanceId();

            // Instead of completing, we add a comment/note and reassign or leave pending
            Map<String, Object> processVariables = new HashMap<>();
            processVariables.put("infoRequested", infoRequested);
            processVariables.put("requestedBy", deferrer);
            processVariables.put("requestTime", new Date());
            processVariables.put("deadline", deadline);
            processVariables.put("workflowStatus", "DEFERRED");
            processVariables.put("processStatus", "AWAITING_MORE_INFO");

            // Set as process variables for workflow tracking
            runtimeService.setVariables(processInstanceId, processVariables);

            // Also set as local task variables
            taskService.setVariablesLocal(taskId, processVariables);

            // Add a comment to the task
            taskService.addComment(taskId, null,
                    "Decision deferred by " + deferrer + ". Info requested: " + infoRequested);

            System.out.println("⏸️ Decision deferred by " + deferrer);
            System.out.println("   Info requested: " + infoRequested);
            System.out.println("   Task ID: " + taskId);
            System.out.println("   Process Instance: " + processInstanceId);
            System.out.println("   workflowStatus: DEFERRED");

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

    // ==================== VIEW SPECIFIC HISTORY TASK ====================
    @GetMapping("/history/task/{taskId}")
    public String viewHistoryTask(@PathVariable String taskId, Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Guest";

        // Get historical task
        HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId)
                .finished()
                .singleResult();

        if (task == null) {
            return "redirect:/ui/department-head/history?error=Task+not+found+in+history";
        }

        // Get historical variables
        Map<String, Object> variables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .list()
                .stream()
                .collect(HashMap::new, (m, v) -> m.put(v.getVariableName(), v.getValue()), HashMap::putAll);

        // Get project details
        Long projectId = null;
        Project project = null;
        if (variables.containsKey("projectId")) {
            projectId = ((Number) variables.get("projectId")).longValue();
            project = projectRepository.findById(projectId).orElse(null);
        }

        model.addAttribute("username", username);
        model.addAttribute("task", task);
        model.addAttribute("variables", variables);
        model.addAttribute("project", project);
        model.addAttribute("projectId", projectId);

        return "department-head/history-detail";
    }

    // ==================== BULK ACTIONS ====================
    @PostMapping("/bulk-approve")
    public String bulkApprove(
            @RequestParam(value = "taskIds", required = false) List<String> taskIds,
            @RequestParam(required = false) String bulkNotes,
            RedirectAttributes redirectAttributes,
            Principal principal) {

        try {
            String approver = principal != null ? principal.getName() : "DepartmentHead";

            if (taskIds == null || taskIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "❌ No tasks selected for bulk approval");
                return "redirect:/ui/department-head/dashboard";
            }

            System.out.println("=== BULK APPROVAL STARTED ===");
            System.out.println("Tasks to approve: " + taskIds.size());
            System.out.println("Task IDs: " + taskIds);

            int successCount = 0;
            int errorCount = 0;
            List<String> errorMessages = new ArrayList<>();

            for (String taskId : taskIds) {
                try {
                    System.out.println("\n--- Processing Task: " + taskId + " ---");

                    Task task = taskService.createTaskQuery()
                            .taskId(taskId.trim())  // Trim whitespace
                            .singleResult();

                    if (task == null) {
                        System.err.println("❌ Task not found: " + taskId);
                        errorCount++;
                        errorMessages.add("Task not found: " + taskId);
                        continue;
                    }

                    System.out.println("✅ Task found: " + task.getName());
                    System.out.println("   Process Instance: " + task.getProcessInstanceId());

                    String processInstanceId = task.getProcessInstanceId();
                    Map<String, Object> currentVariables = runtimeService.getVariables(processInstanceId);

                    // Get project ID for logging
                    Long projectId = null;
                    if (currentVariables.containsKey("projectId")) {
                        projectId = ((Number) currentVariables.get("projectId")).longValue();
                        System.out.println("   Project ID: " + projectId);
                    }

                    // Set approval variables - The delegate will handle status update
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("approved", true);
                    variables.put("approvalNotes", "Bulk approved by " + approver +
                            (bulkNotes != null && !bulkNotes.isEmpty() ? ": " + bulkNotes : ""));
                    variables.put("approvedBy", approver);
                    variables.put("approvalTime", new Date());
                    variables.put("approvedEmployeeIds", new ArrayList<>());

                    // Optional: Set workflow status variables
                    variables.put("workflowStatus", "APPROVED");
                    variables.put("departmentHeadDecision", "APPROVED");
                    variables.put("decisionTimestamp", new Date());
                    variables.put("processStatus", "DEPARTMENT_HEAD_APPROVED");

                    // Complete the task - This will trigger ProjectApprovalDelegate
                    taskService.complete(taskId, variables);

                    System.out.println("✅ Task completed successfully: " + taskId);

                    // Verify completion
                    Task completedTask = taskService.createTaskQuery().taskId(taskId).singleResult();
                    if (completedTask == null) {
                        System.out.println("✅ Task successfully removed from active tasks");
                    }

                    successCount++;

                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Task " + taskId + ": " + e.getMessage();
                    System.err.println("❌ Error: " + errorMsg);
                    e.printStackTrace();
                    errorMessages.add(errorMsg);
                }
            }

            System.out.println("\n=== BULK APPROVAL SUMMARY ===");
            System.out.println("Total tasks: " + taskIds.size());
            System.out.println("Successfully approved: " + successCount);
            System.out.println("Failed: " + errorCount);

            // Prepare result message
            if (errorCount == 0) {
                String message = "✅ Successfully approved " + successCount + " project(s)";
                if (bulkNotes != null && !bulkNotes.isEmpty()) {
                    message += " with notes: " + bulkNotes;
                }
                redirectAttributes.addFlashAttribute("successMessage", message);
            } else if (successCount > 0) {
                String message = "⚠️ Partially completed: " + successCount + " approved, " + errorCount + " failed";
                redirectAttributes.addFlashAttribute("warningMessage", message);
                redirectAttributes.addFlashAttribute("errorDetails", String.join("<br>", errorMessages));
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "❌ Failed to approve any projects. All " + errorCount + " attempts failed.");
                redirectAttributes.addFlashAttribute("errorDetails", String.join("<br>", errorMessages));
            }

        } catch (Exception e) {
            System.err.println("❌ General error in bulk approval: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error in bulk approval: " + e.getMessage());
        }

        return "redirect:/ui/department-head/dashboard";
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
    // ==================== CHECK WORKFLOW STATUS ====================
    @GetMapping("/check-status/{taskId}")
    public String checkWorkflowStatus(
            @PathVariable String taskId,
            RedirectAttributes redirectAttributes) {

        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task != null) {
                String processInstanceId = task.getProcessInstanceId();
                Map<String, Object> variables = runtimeService.getVariables(processInstanceId);

                System.out.println("🔍 CHECKING WORKFLOW STATUS FOR TASK: " + taskId);
                System.out.println("   Process Instance: " + processInstanceId);
                System.out.println("   All Variables: " + variables);
                System.out.println("   workflowStatus: " + variables.get("workflowStatus"));
                System.out.println("   projectStatus: " + variables.get("projectStatus"));
                System.out.println("   approved: " + variables.get("approved"));

                redirectAttributes.addFlashAttribute("infoMessage",
                        "Workflow status checked. See console for details.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Task not found");
            }
        } catch (Exception e) {
            System.err.println("❌ Error checking status: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error checking status: " + e.getMessage());
        }

        return "redirect:/ui/department-head/dashboard";
    }
}