package com.workforce.workforceplanning.service;

import com.workforce.workforceplanning.model.Employee;
import com.workforce.workforceplanning.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final EmployeeRepository repository;

    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    public Employee save(Employee employee) {
        return repository.save(employee);
    }

    public List<Employee> findAll() {
        return repository.findAll();
    }

    public Optional<Employee> findById(Long id) {
        return repository.findById(id);
    }

    // ✅ MAKE SURE THIS METHOD EXISTS
    public List<Employee> searchBySkills(String skills) {
        if (skills == null || skills.trim().isEmpty()) {
            return repository.findAll();
        }

        // Clean and parse the skills string
        String[] searchTerms = skills.toLowerCase()
                .replace(" and ", ",")
                .replace(" & ", ",")
                .split("[,;\\s]+");

        // Convert to lowercase list and remove empty/duplicate terms
        List<String> skillList = Arrays.stream(searchTerms)
                .map(String::trim)
                .filter(skill -> !skill.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (skillList.isEmpty()) {
            return repository.findAll();
        }

        // Get all employees and filter
        return repository.findAll().stream()
                .filter(employee -> employeeHasAnySkill(employee, skillList))
                .collect(Collectors.toList());
    }

    private boolean employeeHasAnySkill(Employee employee, List<String> requiredSkills) {
        if (employee.getSkills() == null || employee.getSkills().isEmpty()) {
            return false;
        }

        // Convert employee skills to lowercase for case-insensitive matching
        Set<String> employeeSkills = employee.getSkills().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Return true if employee has ANY of the required skills
        return requiredSkills.stream()
                .anyMatch(employeeSkills::contains);
    }

    public List<Employee> findAvailable() {
        return repository.findAll().stream()
                .filter(Employee::isAvailableForProject)
                .collect(Collectors.toList());
    }
}