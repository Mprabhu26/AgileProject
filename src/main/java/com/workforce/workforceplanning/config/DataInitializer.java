package com.workforce.workforceplanning.config;

import com.workforce.workforceplanning.model.*;
import com.workforce.workforceplanning.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            EmployeeRepository employeeRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // ✅ ONLY load data if database is empty
        if (userRepository.count() == 0 && employeeRepository.count() == 0) {
            loadSampleData();
        } else {
            System.out.println("⏭️ Database already has data, skipping initialization");
        }
    }

    private void loadSampleData() {
        System.out.println("🚀 Loading sample data...");

        // =========================
        // CREATE USERS (Login Credentials)
        // =========================

        // Project Manager
        User pmUser = new User(
                "pm",
                passwordEncoder.encode("pm123"),
                "ROLE_PROJECT_MANAGER"
        );
        userRepository.save(pmUser);

        // Department Head
        User headUser = new User(
                "head",
                passwordEncoder.encode("head123"),
                "ROLE_DEPARTMENT_HEAD"
        );
        userRepository.save(headUser);

        // Resource Planner
        User plannerUser = new User(
                "planner",
                passwordEncoder.encode("planner123"),
                "ROLE_RESOURCE_PLANNER"
        );
        userRepository.save(plannerUser);

        // =========================
        // CREATE EMPLOYEES WITH LOGIN CREDENTIALS
        // =========================

        // Employee 1: John Doe
        Employee emp1 = new Employee();
        emp1.setName("John Doe");
        emp1.setEmail("john@company.com");
        emp1.setSkills(Set.of("Java", "Spring Boot", "PostgreSQL"));
        emp1.setDepartment("IT");
        emp1.setRole("Senior Developer");
        emp1.setAvailable(true);
        emp1 = employeeRepository.save(emp1);

        User emp1User = new User(
                "john",
                passwordEncoder.encode("emp123"),
                "ROLE_EMPLOYEE"
        );
        emp1User.setEmployee(emp1);
        userRepository.save(emp1User);

        // Employee 2: Jane Smith
        Employee emp2 = new Employee();
        emp2.setName("Jane Smith");
        emp2.setEmail("jane@company.com");
        emp2.setSkills(Set.of("React", "JavaScript", "CSS", "TypeScript"));
        emp2.setDepartment("IT");
        emp2.setRole("Frontend Developer");
        emp2.setAvailable(true);
        emp2 = employeeRepository.save(emp2);

        User emp2User = new User(
                "jane",
                passwordEncoder.encode("emp123"),
                "ROLE_EMPLOYEE"
        );
        emp2User.setEmployee(emp2);
        userRepository.save(emp2User);

        // Employee 3: Bob Wilson
        Employee emp3 = new Employee();
        emp3.setName("Bob Wilson");
        emp3.setEmail("bob@company.com");
        emp3.setSkills(Set.of("Python", "Django", "MySQL", "AWS"));
        emp3.setDepartment("IT");
        emp3.setRole("Backend Developer");
        emp3.setAvailable(false);
        emp3 = employeeRepository.save(emp3);

        User emp3User = new User(
                "bob",
                passwordEncoder.encode("emp123"),
                "ROLE_EMPLOYEE"
        );
        emp3User.setEmployee(emp3);
        userRepository.save(emp3User);

        // =========================
        // CREATE PROJECTS
        // =========================

        Project bankingProject = new Project();
        bankingProject.setName("Banking Project");
        bankingProject.setDescription("Core banking system modernization");
        bankingProject.setStatus(ProjectStatus.APPROVED);
        bankingProject.setStartDate(LocalDate.of(2025, 1, 15));
        bankingProject.setEndDate(LocalDate.of(2025, 6, 30));
        bankingProject.setBudget(new BigDecimal("1500000"));
        bankingProject.setTotalEmployeesRequired(5);
        bankingProject.setCreatedBy("pm");
        bankingProject.setPublished(true);
        bankingProject.setVisibleToAll(true);

        ProjectSkillRequirement req1 = new ProjectSkillRequirement(bankingProject, "Java", 2);
        ProjectSkillRequirement req2 = new ProjectSkillRequirement(bankingProject, "Python", 2);
        ProjectSkillRequirement req3 = new ProjectSkillRequirement(bankingProject, "React", 1);

        bankingProject.addSkillRequirement(req1);
        bankingProject.addSkillRequirement(req2);
        bankingProject.addSkillRequirement(req3);

        projectRepository.save(bankingProject);

        System.out.println("✅ Sample data loaded successfully");
        System.out.println("📊 Users created:");
        System.out.println("   - pm / pm123 (Project Manager)");
        System.out.println("   - head / head123 (Department Head)");
        System.out.println("   - planner / planner123 (Resource Planner)");
        System.out.println("   - john / emp123 (Employee)");
        System.out.println("   - jane / emp123 (Employee)");
        System.out.println("   - bob / emp123 (Employee)");
    }
}