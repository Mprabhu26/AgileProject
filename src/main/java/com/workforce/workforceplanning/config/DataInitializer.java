package com.workforce.workforceplanning.config;

import com.workforce.workforceplanning.model.*;
import com.workforce.workforceplanning.repository.EmployeeRepository;
import com.workforce.workforceplanning.repository.ProjectRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        if (employeeRepository.count() == 0 && projectRepository.count() == 0) {
            loadSampleData();
        }
    }

    private void loadSampleData() {

        System.out.println("🚀 Loading sample data...");

        // =========================
        // EMPLOYEES
        // =========================

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

        // =========================
        // PROJECT 1 — BANKING
        // =========================

        Project bankingProject = new Project();
        bankingProject.setName("Banking Project");
        bankingProject.setDescription("Core banking system");
        bankingProject.setStatus(ProjectStatus.PENDING);
        bankingProject.setStartDate(LocalDate.of(2025, 1, 15));
        bankingProject.setEndDate(LocalDate.of(2025, 6, 30));
        bankingProject.setBudget(new BigDecimal("1500000"));
        bankingProject.setTotalEmployeesRequired(5);

        bankingProject.getSkillRequirements().add(
                new ProjectSkillRequirement(bankingProject, "Java", 2));
        bankingProject.getSkillRequirements().add(
                new ProjectSkillRequirement(bankingProject, "Python", 2));
        bankingProject.getSkillRequirements().add(
                new ProjectSkillRequirement(bankingProject, "React", 1));

        projectRepository.save(bankingProject);

        // =========================
        // PROJECT 2 — IOT
        // =========================

        Project iotProject = new Project();
        iotProject.setName("IoT Platform");
        iotProject.setDescription("IoT analytics and monitoring platform");
        iotProject.setStatus(ProjectStatus.PENDING);
        iotProject.setStartDate(LocalDate.of(2025, 2, 1));
        iotProject.setEndDate(LocalDate.of(2025, 9, 30));
        iotProject.setBudget(new BigDecimal("2200000"));
        iotProject.setTotalEmployeesRequired(4);

        iotProject.getSkillRequirements().add(
                new ProjectSkillRequirement(iotProject, "Java", 2));
        iotProject.getSkillRequirements().add(
                new ProjectSkillRequirement(iotProject, "Python", 1));
        iotProject.getSkillRequirements().add(
                new ProjectSkillRequirement(iotProject, "React", 1));

        projectRepository.save(iotProject);

        System.out.println("✅ Sample data loaded successfully");
    }
}
