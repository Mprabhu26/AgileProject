package com.workforce.workforceplanning.config;

import com.workforce.workforceplanning.model.*;
import com.workforce.workforceplanning.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;

    public DataInitializer(EmployeeRepository employeeRepository,
                           ProjectRepository projectRepository) {
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public void run(String... args) {
        // Only load if database is empty
        if (employeeRepository.count() == 0) {
            loadSampleData();
        }
    }

    private void loadSampleData() {
        System.out.println("Loading sample data...");

        // Create Employees
        Employee emp1 = new Employee();
        emp1.setName("John Doe");
        emp1.setEmail("john@company.com");
        emp1.setSkills(Set.of("Java", "Spring Boot", "PostgreSQL"));
        emp1.setDepartment("IT");
        emp1.setAvailable(true);
        employeeRepository.save(emp1);

        Employee emp2 = new Employee();
        emp2.setName("Jane Smith");
        emp2.setEmail("jane@company.com");
        emp2.setSkills(Set.of("React", "JavaScript", "CSS"));
        emp2.setDepartment("IT");
        emp2.setAvailable(true);
        employeeRepository.save(emp2);

        Employee emp3 = new Employee();
        emp3.setName("Bob Wilson");
        emp3.setEmail("bob@company.com");
        emp3.setSkills(Set.of("Python", "Django", "MySQL"));
        emp3.setDepartment("IT");
        emp3.setAvailable(false);
        employeeRepository.save(emp3);

        // Create Projects
        Project proj1 = new Project();
        proj1.setName("Website Redesign");
        proj1.setDescription("Redesign company website");
        proj1.setRequiredSkills(Set.of("React", "JavaScript"));
        proj1.setStatus(ProjectStatus.PENDING);
        projectRepository.save(proj1);

        Project proj2 = new Project();
        proj2.setName("Mobile App Development");
        proj2.setDescription("Create mobile app for customers");
        proj2.setRequiredSkills(Set.of("Java", "Spring Boot"));
        proj2.setStatus(ProjectStatus.PENDING);
        projectRepository.save(proj2);

        System.out.println("✅ Sample data loaded: 3 employees, 2 projects");
    }
}