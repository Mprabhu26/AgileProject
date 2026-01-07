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

    // Single creation
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Returns 201 instead of 200
    public Employee createEmployee(@RequestBody Employee employee) {
        return service.save(employee);
    }

    // creation for multiple employees
    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Employee> createEmployees(@RequestBody List<Employee> employees) {
        // This assumes your EmployeeService has a saveAll method
        // which calls repository.saveAll(employees)
        return service.saveAll(employees);
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