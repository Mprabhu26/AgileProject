package com.workforce.workforceplanning.controller;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.repository.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;  // ← Change from @RestController
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller  // ← Change from @RestController
@RequestMapping("/projects")
public class ProjectPublishController {

    private final ProjectRepository projectRepository;

    public ProjectPublishController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    public String publishProject(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {  // ← Add RedirectAttributes

        return projectRepository.findById(id)
                .map(project -> {
                    project.setPublished(true);
                    project.setVisibleToAll(true);
                    project.setPublishedAt(LocalDateTime.now());
                    projectRepository.save(project);

                    // Add success message
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Project '" + project.getName() + "' published successfully!");

                    // Redirect back to projects list
                    return "redirect:/ui/projects";
                })
                .orElse("redirect:/ui/projects?error=Project+not+found");
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    public String unpublishProject(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        return projectRepository.findById(id)
                .map(project -> {
                    project.setPublished(false);
                    project.setVisibleToAll(false);
                    projectRepository.save(project);

                    redirectAttributes.addFlashAttribute("successMessage",
                            "Project '" + project.getName() + "' unpublished successfully!");

                    return "redirect:/ui/projects";
                })
                .orElse("redirect:/ui/projects?error=Project+not+found");
    }
}