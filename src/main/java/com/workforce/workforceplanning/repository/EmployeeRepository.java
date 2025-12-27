package com.workforce.workforceplanning.repository;

import com.workforce.workforceplanning.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
}
