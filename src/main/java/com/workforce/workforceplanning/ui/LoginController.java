package com.workforce.workforceplanning.ui;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password!");
            System.out.println("⚠️ Login attempt failed");
        }

        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
            System.out.println("✅ User logged out");
        }

        // Add test user info for debugging
        model.addAttribute("testUsers", getTestUsers());

        return "login";
    }

    private String getTestUsers() {
        return """
            Test Users:
            • pm / pm123 (Project Manager)
            • head / head123 (Department Head)
            • planner / planner123 (Resource Planner)
            • john / emp123 (Employee)
            • jane / emp123 (Employee)
            """;
    }
}