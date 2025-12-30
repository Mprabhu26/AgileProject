package com.workforce.workforceplanning.service;

import com.workforce.workforceplanning.model.Employee;
import com.workforce.workforceplanning.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import java.util.Optional;
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

    public List<Employee> searchBySkills(String skills) {
        String[] requiredSkills = skills.toLowerCase().split(",");

        return repository.findAll().stream()
                .filter(employee -> {
                    for (String requiredSkill : requiredSkills) {
                        String skill = requiredSkill.trim();
                        boolean hasSkill = employee.getSkills().stream()
                                .anyMatch(s -> s.equalsIgnoreCase(skill));
                        if (hasSkill) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    public List<Employee> findAvailable() {
        return repository.findAll().stream()
                .filter(Employee::isAvailableForProject)
                .collect(Collectors.toList());
    }


}

