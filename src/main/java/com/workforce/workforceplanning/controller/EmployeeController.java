package com.workforce.workforceplanning.controller;

import com.workforce.workforceplanning.model.Employee;
import com.workforce.workforceplanning.service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    // Remove one of these duplicate methods. Keep this one with @ResponseStatus:
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Returns 201 instead of 200
    public Employee createEmployee(@RequestBody Employee employee) {
        return service.save(employee);
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
}