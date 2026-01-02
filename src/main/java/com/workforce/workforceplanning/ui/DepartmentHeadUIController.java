package com.workforce.workforceplanning.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ui/department-head")
public class DepartmentHeadUIController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "department-head/dashboard";
    }
}
