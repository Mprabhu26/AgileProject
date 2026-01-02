package com.workforce.workforceplanning.ui;

import com.workforce.workforceplanning.dto.CreateProjectForm;
import com.workforce.workforceplanning.dto.SkillRequirementDto;
import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.model.ProjectSkillRequirement;
import com.workforce.workforceplanning.model.ProjectStatus;
import com.workforce.workforceplanning.repository.ProjectRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.ArrayList;

@Controller
@RequestMapping("/ui/projects")
public class ProjectUiController {

    private final ProjectRepository projectRepository;

    public ProjectUiController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @GetMapping("/create")
    public String showCreateForm(Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Guest";
        model.addAttribute("username", username);
        model.addAttribute("projectForm", new CreateProjectForm());
        return "projects/create";
    }

    @PostMapping("/create")
    public String createProject(
            @ModelAttribute CreateProjectForm form,
            Principal principal,
            Model model
    ) {
        try {
            String username = principal != null ? principal.getName() : "Guest";
            model.addAttribute("username", username);

            Project project = new Project();
            project.setName(form.getName());
            project.setDescription(form.getDescription());
            project.setStartDate(form.getStartDate());
            project.setEndDate(form.getEndDate());
            project.setBudget(form.getBudget());
            project.setTotalEmployeesRequired(form.getTotalEmployeesRequired());
            project.setCreatedBy(username);
            project.setStatus(ProjectStatus.PENDING);

            // ✅ FIXED: Initialize skill requirements list
            project.setSkillRequirements(new ArrayList<>());

            // ✅ FIXED: Handle skill requirements properly
            if (form.getSkillRequirements() != null) {
                for (SkillRequirementDto dto : form.getSkillRequirements()) {
                    // Check if skill is provided (not empty)
                    if (dto != null &&
                            dto.getSkill() != null &&
                            !dto.getSkill().trim().isEmpty()) {

                        // Get count - try different methods
                        Integer count = dto.getRequiredCount();

                        // If count is null or invalid, use default of 1
                        if (count == null || count < 1) {
                            count = 1;
                        }

                        // Create and add the requirement
                        ProjectSkillRequirement requirement = new ProjectSkillRequirement();
                        requirement.setSkill(dto.getSkill().trim());
                        requirement.setRequiredCount(count);  // Use the count value
                        requirement.setProject(project);      // Set bidirectional relationship

                        project.getSkillRequirements().add(requirement);
                    }
                }
            }

            projectRepository.save(project);
            return "redirect:/ui/projects";
        } catch (Exception e) {
            model.addAttribute("error", "Error creating project: " + e.getMessage());
            model.addAttribute("projectForm", form);
            if (principal != null) {
                model.addAttribute("username", principal.getName());
            }
            return "projects/create";
        }
    }

    @GetMapping
    public String myProjects(Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Guest";
        model.addAttribute("username", username);
        model.addAttribute(
                "projects",
                projectRepository.findByCreatedBy(username)
        );
        return "projects/list";
    }
}