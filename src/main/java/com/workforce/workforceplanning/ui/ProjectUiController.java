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

@Controller
@RequestMapping("/ui/projects")
public class ProjectUiController {

    private final ProjectRepository projectRepository;

    public ProjectUiController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // =====================
    // SHOW CREATE FORM
    // =====================
    @GetMapping("/create")
    public String showCreateForm(Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Guest";
        model.addAttribute("username", username);
        model.addAttribute("projectForm", new CreateProjectForm());
        return "projects/create";
    }

    // =====================
    // HANDLE SUBMIT
    // =====================
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

            // ✅ FIXED: Use getRequiredCount() if that's what your DTO has
            if (form.getSkillRequirements() != null) {
                for (SkillRequirementDto dto : form.getSkillRequirements()) {
                    // Check which method your DTO has
                    Integer count = null;

                    // Try getRequiredCount() first
                    try {
                        count = dto.getRequiredCount();
                    } catch (Exception e) {
                        // If not, try getCount()
                        count = dto.getCount();
                    }

                    if (dto.getSkill() != null && !dto.getSkill().trim().isEmpty() &&
                            count != null && count > 0) {
                        ProjectSkillRequirement req = new ProjectSkillRequirement(
                                project,
                                dto.getSkill(),
                                count  // Use the correct count
                        );
                        project.getSkillRequirements().add(req);
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

    // =====================
    // MY PROJECTS (PM)
    // =====================
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