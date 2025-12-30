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

@RestController
@RequestMapping("/workflow")
public class WorkflowController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public WorkflowController(RuntimeService runtimeService, TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
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
}
