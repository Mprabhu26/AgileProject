package com.workforce.workforceplanning.controller;

import com.workforce.workforceplanning.model.Employee;
import com.workforce.workforceplanning.service.EmployeeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @PostMapping
    public Employee createEmployee(@RequestBody Employee employee) {
        return service.save(employee);
    }

    @GetMapping
    public List<Employee> getAllEmployees() {
        return service.findAll();
    }
}
