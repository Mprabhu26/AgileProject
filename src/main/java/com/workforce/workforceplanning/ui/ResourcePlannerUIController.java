package com.workforce.workforceplanning.ui;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.model.Employee;
import com.workforce.workforceplanning.model.ProjectStatus;
import com.workforce.workforceplanning.repository.ProjectRepository;
import com.workforce.workforceplanning.repository.EmployeeRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ui/resource-planner")
public class ResourcePlannerUIController {

    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;

    public ResourcePlannerUIController(
            ProjectRepository projectRepository,
            EmployeeRepository employeeRepository
    ) {
        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            // Get all projects
            List<Project> allProjects = projectRepository.findAll();

            // Filter for published AND approved projects
            List<Project> availableProjects = allProjects.stream()
                    .filter(p -> p.getPublished() != null && p.getPublished())
                    .filter(p -> p.getStatus() == ProjectStatus.APPROVED)
                    .collect(Collectors.toList());

            // Get available employees
            List<Employee> availableEmployees = employeeRepository.findAll().stream()
                    .filter(e -> e.getAvailable() != null && e.getAvailable())
                    .collect(Collectors.toList());

            // Add to model
            model.addAttribute("availableProjects", availableProjects);
            model.addAttribute("availableEmployees", availableEmployees);

            return "projects/dashboard";

        } catch (Exception e) {
            // If there's an error, return empty lists
            model.addAttribute("availableProjects", List.of());
            model.addAttribute("availableEmployees", List.of());
            return "projects/dashboard";
        }
    }
}