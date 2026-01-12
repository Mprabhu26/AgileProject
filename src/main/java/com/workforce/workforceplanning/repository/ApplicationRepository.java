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

    // Find applications by project ID
    List<Application> findByProjectId(Long projectId);

    // Find applications by project ID and status
    List<Application> findByProjectIdAndStatus(Long projectId, ApplicationStatus status);

    // Find application by project and employee (to prevent duplicates)
    Optional<Application> findByProjectIdAndEmployeeId(Long projectId, Long employeeId);

    // Find pending applications for a project
    List<Application> findByProjectIdAndStatusOrderByAppliedAtDesc(Long projectId, ApplicationStatus status);

    // Find applications by employee ID
    List<Application> findByEmployeeId(Long employeeId);

    // Find applications by employee ID and status
    List<Application> findByEmployeeIdAndStatus(Long employeeId, ApplicationStatus status);

    // Count pending applications for a project
    long countByProjectIdAndStatus(Long projectId, ApplicationStatus status);

    // Find assignments by status
    List<Application> findByStatus(ApplicationStatus status);


    // Check if employee is assigned to project
    boolean existsByProjectIdAndEmployeeId(Long projectId, Long employeeId);
    List<Application> findByEmployeeIdOrderByAppliedAtDesc(Long employeeId);
    long countByProjectId(Long projectId);

    // Custom query to find applications for projects created by specific user
    @Query("SELECT a FROM Application a WHERE a.project.createdBy = :createdBy AND a.status = :status")
    List<Application> findByProjectCreatedByAndStatus(
            @Param("createdBy") String createdBy,
            @Param("status") ApplicationStatus status);

    // Find all applications with eager loading of project and employee
    @Query("SELECT a FROM Application a JOIN FETCH a.project JOIN FETCH a.employee")
    List<Application> findAllWithDetails();

    // Find assignments for projects created by specific user
    @Query("SELECT a FROM Assignment a WHERE a.project.createdBy = :createdBy")
    List<Assignment> findByProjectCreatedBy(@Param("createdBy") String createdBy);

    // Find active assignments (not completed or cancelled)
    @Query("SELECT a FROM Assignment a WHERE a.status IN ('ASSIGNED', 'IN_PROGRESS')")
    List<Assignment> findActiveAssignments();
}