package com.workforce.workforceplanning.repository;

import com.workforce.workforceplanning.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;  // ← ADD THIS LINE!

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByEmployeeId(Long employeeId);
    boolean existsByProjectIdAndEmployeeId(Long projectId, Long employeeId);
}