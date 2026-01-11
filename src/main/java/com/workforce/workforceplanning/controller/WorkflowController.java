package com.workforce.workforceplanning.controller;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricActivityInstance;

@RestController
@RequestMapping("/workflow")
public class WorkflowController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    public WorkflowController(RuntimeService runtimeService, TaskService taskService, HistoryService historyService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
    }

    // ▶ START WORKFLOW
    @PostMapping("/start/{projectId}")
    public ResponseEntity<Map<String, Object>> startWorkflow(@PathVariable Long projectId) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("projectId", projectId);

        var processInstance = runtimeService.startProcessInstanceByKey(
                "workforcePlanningProcess",
                variables
        );

        log.info("✅ Workflow started | processInstanceId={} | projectId={}",
                processInstance.getId(), projectId);

        return ResponseEntity.ok(Map.of(
                "message", "Workflow started",
                "processInstanceId", processInstance.getId(),
                "projectId", projectId
        ));
    }

    // ▶ GET ALL ACTIVE TASKS
    @GetMapping("/tasks")
    public ResponseEntity<List<Map<String, Object>>> getTasks() {

        List<Task> tasks = taskService.createTaskQuery()
                .active()
                .list();

        List<Map<String, Object>> response = tasks.stream().map(task -> {
            Map<String, Object> map = new HashMap<>();
            map.put("taskId", task.getId());
            map.put("name", task.getName());
            map.put("assignee", task.getAssignee());
            map.put("processInstanceId", task.getProcessInstanceId());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ▶ NEW: GET TASKS FOR SPECIFIC ROLE (Resource Planner, Department Head, etc.)
    @GetMapping("/tasks/role/{role}")
    public ResponseEntity<List<Map<String, Object>>> getTasksByRole(@PathVariable String role) {

        List<Task> tasks = taskService.createTaskQuery()
                .taskCandidateGroup(role)  // Gets tasks assigned to this role
                .active()
                .list();

        List<Map<String, Object>> response = tasks.stream().map(task -> {
            // Get process variables to show more context
            Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());

            Map<String, Object> map = new HashMap<>();
            map.put("taskId", task.getId());
            map.put("name", task.getName());
            map.put("assignee", task.getAssignee());
            map.put("processInstanceId", task.getProcessInstanceId());
            map.put("projectId", variables.get("projectId"));
            map.put("createTime", task.getCreateTime());
            return map;
        }).collect(Collectors.toList());

        log.info("Found {} tasks for role: {}", response.size(), role);
        return ResponseEntity.ok(response);
    }

    // ▶ APPROVE TASK (Specific endpoint for approval)
    @PostMapping("/tasks/{taskId}/approve")
    public ResponseEntity<Map<String, Object>> approveTask(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Object> requestBody
    ) {
        log.info("➡ Approving task {}", taskId);

        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Task not found",
                    "taskId", taskId
            ));
        }

        String processInstanceId = task.getProcessInstanceId();

        // Get existing variables
        Map<String, Object> variables = new HashMap<>(
                runtimeService.getVariables(processInstanceId)
        );

        // Set approved = true
        variables.put("approved", true);

        // Add any additional data from request body
        if (requestBody != null) {
            variables.putAll(requestBody);
        }

        log.info("✅ Approving with variables: {}", variables);

        try {
            taskService.complete(taskId, variables);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Task approved successfully",
                    "taskId", taskId,
                    "taskName", task.getName(),
                    "approved", true
            ));

        } catch (Exception e) {
            log.error("❌ Error approving task", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Task approval failed",
                    "message", e.getMessage()
            ));
        }
    }

    // ▶ NEW: REJECT TASK (Specific endpoint for rejection)
    @PostMapping("/tasks/{taskId}/reject")
    public ResponseEntity<Map<String, Object>> rejectTask(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Object> requestBody
    ) {
        log.info("➡ Rejecting task {}", taskId);

        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Task not found",
                    "taskId", taskId
            ));
        }

        String processInstanceId = task.getProcessInstanceId();

        // Get existing variables
        Map<String, Object> variables = new HashMap<>(
                runtimeService.getVariables(processInstanceId)
        );

        // Set approved = false
        variables.put("approved", false);

        // Add rejection reason if provided
        if (requestBody != null) {
            if (requestBody.containsKey("reason")) {
                variables.put("rejectionReason", requestBody.get("reason"));
            }
            variables.putAll(requestBody);
        }

        log.info("❌ Rejecting with variables: {}", variables);

        try {
            taskService.complete(taskId, variables);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Task rejected successfully",
                    "taskId", taskId,
                    "taskName", task.getName(),
                    "approved", false,
                    "reason", variables.getOrDefault("rejectionReason", "No reason provided")
            ));

        } catch (Exception e) {
            log.error("❌ Error rejecting task", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Task rejection failed",
                    "message", e.getMessage()
            ));
        }
    }

    // ▶ COMPLETE TASK (UNIVERSAL – WORKS FOR ALL USER TASKS)
    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<Map<String, Object>> completeTask(
            @PathVariable String taskId,
            @RequestParam(required = false) Boolean approved,
            @RequestBody(required = false) Map<String, Object> requestBody
    ) {

        log.info("➡ Completing task {}", taskId);

        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Task not found",
                    "taskId", taskId
            ));
        }

        String processInstanceId = task.getProcessInstanceId();

        // 🔹 ALWAYS FETCH EXISTING VARIABLES
        Map<String, Object> variables = new HashMap<>(
                runtimeService.getVariables(processInstanceId)
        );

        // 🔹 ADD approved IF PRESENT
        if (approved != null) {
            variables.put("approved", approved);
        }

        // 🔹 ADD BODY VARIABLES (employeeIds etc.)
        if (requestBody != null) {
            variables.putAll(requestBody);
        }

        log.info("Final variables passed to Flowable: {}", variables);

        try {
            taskService.complete(taskId, variables);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Task completed successfully",
                    "taskId", taskId,
                    "taskName", task.getName()
            ));

        } catch (Exception e) {
            log.error("❌ Error completing task", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Task completion failed",
                    "message", e.getMessage()
            ));
        }
    }

    // ▶ GET SINGLE TASK DETAILS
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable String taskId) {

        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Task not found"
            ));
        }

        Map<String, Object> variables =
                runtimeService.getVariables(task.getProcessInstanceId());

        Map<String, Object> response = new HashMap<>();
        response.put("taskId", task.getId());
        response.put("name", task.getName());
        response.put("assignee", task.getAssignee());
        response.put("processInstanceId", task.getProcessInstanceId());
        response.put("variables", variables);

        return ResponseEntity.ok(response);
    }

    // ▶ NEW: GET WORKFLOW HISTORY (For transparency - see who approved/rejected)
    @GetMapping("/history/{processInstanceId}")
    public ResponseEntity<List<Map<String, Object>>> getWorkflowHistory(
            @PathVariable String processInstanceId) {

        List<HistoricActivityInstance> activities = historyService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();

        List<Map<String, Object>> history = activities.stream()
                .map(activity -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("activityId", activity.getActivityId());
                    info.put("activityName", activity.getActivityName());
                    info.put("activityType", activity.getActivityType());
                    info.put("assignee", activity.getAssignee());
                    info.put("startTime", activity.getStartTime());
                    info.put("endTime", activity.getEndTime());
                    info.put("durationMs", activity.getDurationInMillis());
                    return info;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    // ▶ NEW: GET WORKFLOW STATISTICS
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {

        long activeCount = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("workforcePlanningProcess")
                .active()
                .count();

        long completedCount = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey("workforcePlanningProcess")
                .finished()
                .count();

        long activeTasksCount = taskService.createTaskQuery()
                .active()
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeWorkflows", activeCount);
        stats.put("completedWorkflows", completedCount);
        stats.put("totalWorkflows", activeCount + completedCount);
        stats.put("activeTasks", activeTasksCount);

        return ResponseEntity.ok(stats);
    }
}
