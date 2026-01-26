package com.workforce.workforceplanning.repository;

import com.workforce.workforceplanning.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.workforce.workforceplanning.model.ProjectStatus;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByPublishedTrueAndWorkflowStatus(String workflowStatus);

    List<Project> findByCreatedBy(String createdBy);


    @Query("SELECT p FROM Project p WHERE p.published = true AND p.status = :status AND p.workflowStatus = :workflowStatus")
    List<Project> findPublishedProjectsForApproval(
            @Param("status") ProjectStatus status,
            @Param("workflowStatus") String workflowStatus
    );

    // =========================
    // For Dept Head Dashboard
    // =========================
    @Query("""
        SELECT p FROM Project p
        WHERE p.published = true
          AND p.status = :status
    """)
    List<Project> findPublishedByStatus(
            @Param("status") ProjectStatus status
    );




}