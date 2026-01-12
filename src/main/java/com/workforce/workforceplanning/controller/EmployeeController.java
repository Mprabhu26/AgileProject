package com.workforce.workforceplanning.controller;

import com.workforce.workforceplanning.model.Employee;
import com.workforce.workforceplanning.model.User;  // ← ADD THIS
import com.workforce.workforceplanning.repository.EmployeeRepository;  // ← ADD THIS
import com.workforce.workforceplanning.repository.UserRepository;
import com.workforce.workforceplanning.service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;  // ← ADD THIS
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService service;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeRepository employeeRepository;  // ← ADD THIS

    // Constructor injection
    public EmployeeController(
            EmployeeService service,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmployeeRepository employeeRepository) {  // ← ADD THIS PARAMETER
        this.service = service;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.employeeRepository = employeeRepository;  // ← INITIALIZE THIS
    }

    /**
     * CREATE EMPLOYEE + LOGIN CREDENTIALS
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> createEmployee(@RequestBody Employee employee) {

        System.out.println("🔹 Creating employee: " + employee.getName());

        // 1. Save employee record
        Employee savedEmployee = service.save(employee);
        System.out.println("✅ Employee saved with ID: " + savedEmployee.getId());

        // 2. Generate username from email
        String username = employee.getEmail().split("@")[0];
        System.out.println("🔹 Generated username: " + username);

        // 3. Check if username already exists
        if (userRepository.existsByUsername(username)) {
            System.out.println("⚠️ Username already exists: " + username);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "Username already exists: " + username,
                            "message", "Employee created but login credentials NOT created. Change email and try again.",
                            "employee", savedEmployee
                    ));
        }

        // 4. Create login credentials with DEFAULT password
        String defaultPassword = "employee123";
        String hashedPassword = passwordEncoder.encode(defaultPassword);

        User user = new User(username, hashedPassword, "ROLE_EMPLOYEE");
        user.setEmployee(savedEmployee);  // Link to employee
        userRepository.save(user);

        System.out.println("✅ User account created for: " + username);

        // 5. Return success with login info
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "success", true,
                        "message", "Employee created successfully with login credentials",
                        "employee", savedEmployee,
                        "loginCredentials", Map.of(
                                "username", username,
                                "password", defaultPassword,
                                "note", "Please change password after first login"
                        )
                ));
    }

    /**
     * CREATE MULTIPLE EMPLOYEES
     */
    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> createEmployees(@RequestBody List<Employee> employees) {

        List<Map<String, Object>> results = new ArrayList<>();

        for (Employee employee : employees) {
            // 1. Save employee
            Employee savedEmployee = service.save(employee);

            // 2. Generate username
            String username = employee.getEmail().split("@")[0];

            // 3. Create login credentials (skip if username exists)
            if (!userRepository.existsByUsername(username)) {
                User user = new User(
                        username,
                        passwordEncoder.encode("employee123"),
                        "ROLE_EMPLOYEE"
                );
                user.setEmployee(savedEmployee);
                userRepository.save(user);

                results.add(Map.of(
                        "employee", savedEmployee,
                        "username", username,
                        "password", "employee123"
                ));
            } else {
                results.add(Map.of(
                        "employee", savedEmployee,
                        "warning", "Login credentials already exist for: " + username
                ));
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Employees created successfully",
                        "count", results.size(),
                        "results", results
                ));
    }

    @GetMapping
    public List<Employee> getAllEmployees() {
        return service.findAll();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Employee>> searchBySkills(@RequestParam String skills) {
        return ResponseEntity.ok(service.searchBySkills(skills));
    }

    @GetMapping("/available")
    public ResponseEntity<List<Employee>> getAvailableEmployees() {
        return ResponseEntity.ok(service.findAvailable());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE EMPLOYEE
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        if (!service.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }

        // 1. Find and delete associated user first
        userRepository.findAll().stream()
                .filter(u -> u.getEmployee() != null && u.getEmployee().getId().equals(id))
                .findFirst()
                .ifPresent(userRepository::delete);

        // 2. Then delete employee
        employeeRepository.deleteById(id);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Employee deleted successfully",
                "employeeId", id
        ));
    }
}