package com.workforce.workforceplanning.ui;

import com.workforce.workforceplanning.repository.ProjectRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ui/projects")
public class ProjectUiController {

    private final ProjectRepository projectRepository;

    public ProjectUiController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("projects", projectRepository.findAll());
        return "projects/list";
    }
}
