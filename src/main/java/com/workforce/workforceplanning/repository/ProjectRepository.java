package com.workforce.workforceplanning.repository;

import com.workforce.workforceplanning.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByCreatedBy(String createdBy);

}

