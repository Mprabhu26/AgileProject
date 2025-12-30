package com.workforce.workforceplanning.controller;

import java.security.Principal;
import com.workforce.workforceplanning.dto.CreateProjectRequest;
import com.workforce.workforceplanning.dto.SkillRequirementDto;
import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.model.ProjectStatus;
import com.workforce.workforceplanning.model.ProjectSkillRequirement;
import com.workforce.workforceplanning.repository.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;

    public ProjectController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // ✅ CREATE PROJECT
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

        // ✅ FIXED: Handle null skill requirements
        if (request.getSkillRequirements() != null) {
            for (SkillRequirementDto dto : request.getSkillRequirements()) {
                if (dto.getSkill() != null && !dto.getSkill().trim().isEmpty() &&
                        dto.getCount() != null && dto.getRequiredCount() > 0) {
                    project.getSkillRequirements().add(
                            new ProjectSkillRequirement(project, dto.getSkill(), dto.getRequiredCount())
                    );
                }
            }
        }

        return ResponseEntity.ok(projectRepository.save(project));
    }

    // ✅ GET ALL PROJECTS
    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.ok(projectRepository.findAll());
    }

    // ✅ GET PROJECT BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getProjectById(@PathVariable Long id) {
        return projectRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}