package com.workforce.workforceplanning.ui;

import com.workforce.workforceplanning.model.Employee;
import com.workforce.workforceplanning.model.User;
import com.workforce.workforceplanning.repository.EmployeeRepository;
import com.workforce.workforceplanning.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/register")
public class EmployeeRegistrationController {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeRegistrationController(
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Show registration form (PUBLIC - no login required)
     */
    @GetMapping
    public String showRegistrationForm(Model model) {
        return "register/employee-registration";
    }

    /**
     * Process employee self-registration
     */
    @PostMapping
    public String registerEmployee(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String department,
            @RequestParam String role,
            @RequestParam String skills,
            @RequestParam(required = false, defaultValue = "true") boolean available,
            RedirectAttributes redirectAttributes) {

        try {
            // Generate username
            String username = email.split("@")[0];

            // Check if email already exists
            if (employeeRepository.findAll().stream()
                    .anyMatch(e -> e.getEmail().equalsIgnoreCase(email))) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "❌ Email already registered. Please use a different email.");
                return "redirect:/register";
            }

            // Check if username already exists
            if (userRepository.existsByUsername(username)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "❌ Username already exists. Please use a different email.");
                return "redirect:/register";
            }

            // Parse skills
            Set<String> skillSet = Arrays.stream(skills.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());

            // Create employee
            Employee employee = new Employee();
            employee.setName(name);
            employee.setEmail(email);
            employee.setDepartment(department);
            employee.setRole(role);
            employee.setSkills(skillSet);
            employee.setAvailable(available);
            employee = employeeRepository.save(employee);

            // Create user account
            User user = new User(
                    username,
                    passwordEncoder.encode(password),
                    "ROLE_EMPLOYEE"
            );
            user.setEmployee(employee);
            userRepository.save(user);

            System.out.println("✅ New employee registered: " + name + " (username: " + username + ")");

            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Registration successful! You can now login with username: " + username);

            return "redirect:/login";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Registration failed: " + e.getMessage());
            return "redirect:/register";
        }
    }
}