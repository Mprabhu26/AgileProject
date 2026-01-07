package com.workforce.workforceplanning.repository;

import com.workforce.workforceplanning.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
}
