package com.workforce.workforceplanning.controller;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/workflow")
public class WorkflowController {

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public WorkflowController(RuntimeService runtimeService,
                              TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    // 🔹 START WORKFLOW
    @PostMapping("/start/{projectId}")
    public String startWorkflow(@PathVariable Long projectId) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("projectId", projectId);
        variables.put("approved", false);

        runtimeService.startProcessInstanceByKey(
                "workforcePlanningProcess",
                variables
        );

        return "Workflow started for project " + projectId;
    }

    // 🔹 GET ALL ACTIVE TASKS
    @GetMapping("/tasks")
    public List<TaskResponse> getAllTasks() {

        return taskService.createTaskQuery()
                .list()
                .stream()
                .map(task -> new TaskResponse(
                        task.getId(),
                        task.getName(),
                        task.getAssignee(),
                        task.getProcessInstanceId()
                ))
                .collect(Collectors.toList());
    }

    // 🔹 COMPLETE TASK (APPROVE / REJECT)
    @PostMapping("/tasks/{taskId}/complete")
    public String completeTask(
            @PathVariable String taskId,
            @RequestParam boolean approved
    ) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", approved);

        taskService.complete(taskId, variables);

        return "Task " + taskId + " completed. Approved = " + approved;
    }

    // 🔹 RESPONSE DTO
    static class TaskResponse {
        public String taskId;
        public String name;
        public String assignee;
        public String processInstanceId;

        public TaskResponse(String taskId,
                            String name,
                            String assignee,
                            String processInstanceId) {
            this.taskId = taskId;
            this.name = name;
            this.assignee = assignee;
            this.processInstanceId = processInstanceId;
        }
    }
}
