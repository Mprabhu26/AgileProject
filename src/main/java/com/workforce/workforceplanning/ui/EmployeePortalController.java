package com.workforce.workforceplanning.ui;

import com.workforce.workforceplanning.model.Project;
import com.workforce.workforceplanning.repository.ProjectRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;  // ✅ ADD THIS IMPORT

@Controller
@RequestMapping("/ui/employee")
public class EmployeePortalController {

    private final ProjectRepository projectRepository;

    public EmployeePortalController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // =====================
    // HTML RESPONSE
    // =====================
    @GetMapping(value = "/projects", produces = MediaType.TEXT_HTML_VALUE)
    public String browseProjectsHtml(
            @RequestParam(required = false) String skill,
            Principal principal,
            Model model) {

        String username = principal.getName();
        model.addAttribute("username", username);

        System.out.println("🔍 HTML request for skill: '" + skill + "'");

        List<Project> publishedProjects = getPublishedProjects(
                skill != null ? parseSkillSearch(skill) : null);

        System.out.println("📊 Found " + publishedProjects.size() + " projects");

        model.addAttribute("projects", publishedProjects);
        model.addAttribute("totalProjects", publishedProjects.size());
        model.addAttribute("searchSkill", skill);

        return "employee/projects-browse";
    }

    // =====================
// SINGLE PROJECT JSON
// =====================
    @GetMapping(value = "/projects/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Project> getProjectJson(@PathVariable Long id) {
        System.out.println("🔍 GET /projects/" + id + " requested");

        Optional<Project> projectOpt = projectRepository.findById(id);

        if (projectOpt.isEmpty()) {
            System.out.println("❌ Project " + id + " not found in database");
            return ResponseEntity.notFound().build();
        }

        Project project = projectOpt.get();
        System.out.println("✅ Found project: " + project.getName());
        System.out.println("   Published: " + project.getPublished());
        System.out.println("   Visible: " + project.getVisibleToAll());

        // Return with filters
        return projectRepository.findById(id)
                .filter(p -> Boolean.TRUE.equals(p.getPublished()))
                .filter(p -> Boolean.TRUE.equals(p.getVisibleToAll()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // =====================
    // JSON RESPONSE
    // =====================
    @GetMapping(value = "/projects", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Project>> browseProjectsJson(
            @RequestParam(required = false) String skill) {

        System.out.println("📡 JSON API request for skill: '" + skill + "'");

        List<Project> publishedProjects = getPublishedProjects(
                skill != null ? parseSkillSearch(skill) : null);

        System.out.println("📊 Found " + publishedProjects.size() + " projects");

        return ResponseEntity.ok(publishedProjects);
    }

    // =====================
    // HELPER METHODS
    // =====================
    private List<Project> getPublishedProjects(List<String> skills) {
        List<Project> allProjects = projectRepository.findAll();

        // Filter published projects
        List<Project> publishedProjects = allProjects.stream()
                .filter(p -> Boolean.TRUE.equals(p.getPublished()))
                .filter(p -> Boolean.TRUE.equals(p.getVisibleToAll()))
                .collect(Collectors.toList());

        // Filter by skills (if provided)
        if (skills != null && !skills.isEmpty()) {
            // Normalize search skills
            List<String> normalizedSkills = skills.stream()
                    .map(s -> s.toLowerCase().trim())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            System.out.println("🎯 Searching for skills: " + normalizedSkills);

            publishedProjects = publishedProjects.stream()
                    .filter(project -> projectHasAnySkill(project, normalizedSkills))
                    .collect(Collectors.toList());
        }

        return publishedProjects;
    }

    private List<String> parseSkillSearch(String searchQuery) {
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Handle "and" as separator
        String normalized = searchQuery.toLowerCase()
                .replace(" and ", ",")
                .replace(" & ", ",");

        List<String> skills = Arrays.stream(normalized.split("[,;\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> !s.equals("and"))  // Remove leftover "and"
                .filter(s -> !s.equals("or"))   // Remove leftover "or"
                .distinct()
                .collect(Collectors.toList());

        System.out.println("🔄 Parsed '" + searchQuery + "' to: " + skills);
        return skills;
    }

    private boolean projectHasAnySkill(Project project, List<String> searchSkills) {
        // Get project skills (normalized)
        Set<String> projectSkills = project.getSkillRequirements().stream()
                .map(req -> req.getSkill() != null ?
                        req.getSkill().toLowerCase().trim() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        System.out.println("  Project '" + project.getName() + "' has skills: " + projectSkills);

        // Check for ANY exact match
        boolean hasMatch = searchSkills.stream()
                .anyMatch(searchSkill -> projectSkills.contains(searchSkill));

        System.out.println("  Matches search? " + hasMatch);
        return hasMatch;
    }
}