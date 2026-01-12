package com.workforce.workforceplanning.repository;

import com.workforce.workforceplanning.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {


    List<Project> findByCreatedBy(String createdBy);


}