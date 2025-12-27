package com.workforce.workforceplanning.repository;

import com.workforce.workforceplanning.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
