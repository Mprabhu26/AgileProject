package com.workforce.workforceplanning.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String redirectToProjects() {
        return "redirect:/ui/projects";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    // REMOVE THIS METHOD - ProjectUiController handles it
    // @GetMapping("/ui/projects")
    // public String listProjects(Principal principal, Model model) {
    //     return "projects/list";
    // }
}