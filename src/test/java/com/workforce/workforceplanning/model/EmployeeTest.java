package com.workforce.workforceplanning.model;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class EmployeeTest {

    @Test
    public void testEmployeeCreation() {
        // Create employee
        Employee employee = new Employee();
        employee.setName("John Doe");
        employee.setEmail("john@test.com");
        employee.setSkills(Set.of("Java", "Spring Boot"));
        employee.setDepartment("IT");

        // Verify
        assertEquals("John Doe", employee.getName());
        assertEquals("john@test.com", employee.getEmail());
        assertTrue(employee.getAvailable());
        assertEquals(2, employee.getSkills().size());
    }
}