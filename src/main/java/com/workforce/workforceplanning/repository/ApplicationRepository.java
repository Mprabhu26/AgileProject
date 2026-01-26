package com.workforce.workforceplanning.repository;

import com.workforce.workforceplanning.model.Application;
import com.workforce.workforceplanning.model.ApplicationStatus;
import com.workforce.workforceplanning.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // ==================== BASIC QUERIES ====================

    List<Application> findByProjectId(Long projectId);

    List<Application> findByProjectIdAndStatus(Long projectId, ApplicationStatus status);

    Optional<Application> findByProjectIdAndEmployeeId(Long projectId, Long employeeId);

    List<Application> findByEmployeeId(Long employeeId);

    List<Application> findByEmployeeIdAndStatus(Long employeeId, ApplicationStatus status);

    List<Application> findByEmployeeIdOrderByAppliedAtDesc(Long employeeId);

    boolean existsByProjectIdAndEmployeeId(Long projectId, Long employeeId);

    // ==================== COUNTS (IMPORTANT) ====================

    // Pending applications for ONE project
    long countByProjectIdAndStatus(Long projectId, ApplicationStatus status);

    // Pending applications for ALL projects owned by a PM
    long countByStatusAndProject_CreatedBy(
            ApplicationStatus status,
            String createdBy
    );

    // ==================== ADVANCED QUERIES ====================

    @Query("SELECT a FROM Application a WHERE a.project.createdBy = :createdBy AND a.status = :status")
    List<Application> findByProjectCreatedByAndStatus(
            @Param("createdBy") String createdBy,
            @Param("status") ApplicationStatus status);

    @Query("SELECT a FROM Application a JOIN FETCH a.project JOIN FETCH a.employee")
    List<Application> findAllWithDetails();

    // ==================== ASSIGNMENT QUERIES (LEGACY) ====================

    @Query("SELECT a FROM Assignment a WHERE a.project.createdBy = :createdBy")
    List<Assignment> findByProjectCreatedBy(@Param("createdBy") String createdBy);

    @Query("SELECT a FROM Assignment a WHERE a.status IN ('ASSIGNED', 'IN_PROGRESS')")
    List<Assignment> findActiveAssignments();
}
