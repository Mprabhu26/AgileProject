package com.workforce.workforceplanning.controller;

import com.workforce.workforceplanning.dto.CreateProjectRequest;
import com.workforce.workforceplanning.dto.SkillRequirementDto;
import com.workforce.workforceplanning.model.Project;
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
    public ResponseEntity<Project> createProject(@RequestBody CreateProjectRequest request) {

        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setBudget(request.getBudget());
        project.setTotalEmployeesRequired(request.getTotalEmployeesRequired());

        // Convert DTO → Entity
        for (SkillRequirementDto dto : request.getSkillRequirements()) {
            ProjectSkillRequirement req =
                    new ProjectSkillRequirement(project, dto.getSkill(), dto.getCount());
            project.getSkillRequirements().add(req);
        }

        Project savedProject = projectRepository.save(project);
        return ResponseEntity.ok(savedProject);
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
