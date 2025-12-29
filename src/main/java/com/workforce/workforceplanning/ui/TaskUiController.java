package com.workforce.workforceplanning.ui;

import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/ui/tasks")
public class TaskUiController {

    private final TaskService taskService;

    public TaskUiController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public String myTasks(Model model) {

        List<Task> tasks = taskService
                .createTaskQuery()
                .active()
                .list();

        model.addAttribute("tasks", tasks);
        return "tasks/my-tasks";
    }
}
