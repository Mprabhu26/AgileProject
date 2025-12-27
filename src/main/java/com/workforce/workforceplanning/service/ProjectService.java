package com.workforce.workforceplanning.service;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.model.ProjectStatus;
import com.workforce.workforceplanning.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository repository;

    public ProjectService(ProjectRepository repository) {
        this.repository = repository;
    }

    public Project create(Project project) {
        project.setStatus(ProjectStatus.PENDING);
        return repository.save(project);
    }

    public Project getById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public List<Project> findAll() {
        return repository.findAll();
    }

    public Project update(Project project) {
        return repository.save(project);
    }
}
