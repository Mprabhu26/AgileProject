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
        // PROJECT 1 — BANKING (Published)
        // =========================

        Project bankingProject = new Project();
        bankingProject.setName("Banking Project");
        bankingProject.setDescription("Core banking system modernization");
        bankingProject.setStatus(ProjectStatus.PENDING);
        bankingProject.setStartDate(LocalDate.of(2025, 1, 15));
        bankingProject.setEndDate(LocalDate.of(2025, 6, 30));
        bankingProject.setBudget(new BigDecimal("1500000"));
        bankingProject.setTotalEmployeesRequired(5);
        bankingProject.setCreatedBy("pm"); // FIXED: Added created_by
        bankingProject.setPublished(true); // FIXED: Set as published
        bankingProject.setVisibleToAll(true); // FIXED: Set visible

        // FIXED: Properly create and add skill requirements
        ProjectSkillRequirement req1 = new ProjectSkillRequirement(bankingProject, "Java", 2);
        ProjectSkillRequirement req2 = new ProjectSkillRequirement(bankingProject, "Python", 2);
        ProjectSkillRequirement req3 = new ProjectSkillRequirement(bankingProject, "React", 1);

        bankingProject.addSkillRequirement(req1);
        bankingProject.addSkillRequirement(req2);
        bankingProject.addSkillRequirement(req3);

        projectRepository.save(bankingProject);

        // =========================
        // PROJECT 2 — IOT (Not Published)
        // =========================

        Project iotProject = new Project();
        iotProject.setName("IoT Platform");
        iotProject.setDescription("IoT analytics and monitoring platform");
        iotProject.setStatus(ProjectStatus.PENDING);
        iotProject.setStartDate(LocalDate.of(2025, 2, 1));
        iotProject.setEndDate(LocalDate.of(2025, 9, 30));
        iotProject.setBudget(new BigDecimal("2200000"));
        iotProject.setTotalEmployeesRequired(4);
        iotProject.setCreatedBy("pm"); // FIXED: Added created_by
        iotProject.setPublished(false); // FIXED: Not published
        iotProject.setVisibleToAll(false); // FIXED: Not visible

        ProjectSkillRequirement req4 = new ProjectSkillRequirement(iotProject, "Java", 2);
        ProjectSkillRequirement req5 = new ProjectSkillRequirement(iotProject, "Python", 1);
        ProjectSkillRequirement req6 = new ProjectSkillRequirement(iotProject, "React", 1);

        iotProject.addSkillRequirement(req4);
        iotProject.addSkillRequirement(req5);
        iotProject.addSkillRequirement(req6);

        projectRepository.save(iotProject);

        // =========================
        // PROJECT 3 — MOBILE APP (Published)
        // =========================

        Project mobileProject = new Project();
        mobileProject.setName("Mobile Banking App");
        mobileProject.setDescription("iOS and Android banking application");
        mobileProject.setStatus(ProjectStatus.APPROVED); // Different status
        mobileProject.setStartDate(LocalDate.of(2025, 3, 1));
        mobileProject.setEndDate(LocalDate.of(2025, 8, 31));
        mobileProject.setBudget(new BigDecimal("800000"));
        mobileProject.setTotalEmployeesRequired(3);
        mobileProject.setCreatedBy("pm");
        mobileProject.setPublished(true);
        mobileProject.setVisibleToAll(true);

        ProjectSkillRequirement req7 = new ProjectSkillRequirement(mobileProject, "React Native", 2);
        ProjectSkillRequirement req8 = new ProjectSkillRequirement(mobileProject, "Java", 1);

        mobileProject.addSkillRequirement(req7);
        mobileProject.addSkillRequirement(req8);

        projectRepository.save(mobileProject);

        System.out.println("✅ Sample data loaded successfully");
        System.out.println("📊 Created: 3 employees, 3 projects (2 published, 1 not published)");
    }
}