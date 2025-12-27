package com.workforce.workforceplanning.controller;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.service.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @PostMapping
    public Project createProject(@RequestBody Project project) {
        return service.create(project);
    }

    @GetMapping
    public List<Project> getAllProjects() {
        return service.findAll();
    }
}
