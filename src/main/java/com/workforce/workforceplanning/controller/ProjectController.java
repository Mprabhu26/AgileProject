package com.workforce.workforceplanning.controller;

import com.workforce.workforceplanning.dto.CreateProjectRequest;
import com.workforce.workforceplanning.dto.SkillRequirementDto;
import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.model.ProjectStatus;
import com.workforce.workforceplanning.model.ProjectSkillRequirement;
import com.workforce.workforceplanning.repository.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;

    public ProjectController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // ✅ FIXED: Ensure skill requirements are saved properly
    @PostMapping
    public ResponseEntity<?> createProject(
            @RequestBody CreateProjectRequest request,
            Principal principal
    ) {
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setBudget(request.getBudget());
        project.setTotalEmployeesRequired(request.getTotalEmployeesRequired());
        project.setStatus(ProjectStatus.PENDING);
        project.setCreatedBy(principal.getName());

        // ✅ IMPORTANT: Initialize the list
        project.setSkillRequirements(new ArrayList<>());

        // ✅ FIXED: Properly handle skill requirements
        if (request.getSkillRequirements() != null) {
            for (SkillRequirementDto dto : request.getSkillRequirements()) {
                // Validate the DTO
                if (dto != null &&
                        dto.getSkill() != null &&
                        !dto.getSkill().trim().isEmpty() &&
                        dto.getRequiredCount() != null &&
                        dto.getRequiredCount() > 0) {

                    ProjectSkillRequirement requirement = new ProjectSkillRequirement();
                    requirement.setSkill(dto.getSkill().trim());
                    requirement.setRequiredCount(dto.getRequiredCount());
                    requirement.setProject(project); // ✅ Set the bidirectional relationship

                    // Add to project
                    project.getSkillRequirements().add(requirement);
                }
            }
        }

        // ✅ Save and return
        Project savedProject = projectRepository.save(project);
        return ResponseEntity.ok(savedProject);
    }

    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.ok(projectRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable Long id) {
        return projectRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}