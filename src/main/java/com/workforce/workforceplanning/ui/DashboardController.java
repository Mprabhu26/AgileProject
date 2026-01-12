package com.workforce.workforceplanning.ui;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PROJECT_MANAGER"))) {
            return "redirect:/ui/projects/dashboard";  // ← Changed to use existing route
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DEPARTMENT_HEAD"))) {
            return "redirect:/ui/department-head/dashboard";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_RESOURCE_PLANNER"))) {
            return "redirect:/ui/resource-planner/dashboard";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))) {
            return "redirect:/ui/employee/projects";
        }

        return "redirect:/";
    }
}