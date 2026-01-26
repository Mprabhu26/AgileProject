package com.workforce.workforceplanning.ui;

import com.workforce.workforceplanning.model.Employee;
import com.workforce.workforceplanning.repository.EmployeeRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/ui/employees")
public class EmployeeListController {

    private final EmployeeRepository employeeRepository;

    public EmployeeListController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @GetMapping
    public String listEmployees(Model model) {
        List<Employee> employees = employeeRepository.findAll();

        // Calculate statistics
        long totalEmployees = employees.size();
        long availableEmployees = employees.stream()
                .filter(e -> Boolean.TRUE.equals(e.getAvailable()))
                .count();
        long assignedEmployees = totalEmployees - availableEmployees;

        model.addAttribute("employees", employees);
        model.addAttribute("totalEmployees", totalEmployees);
        model.addAttribute("availableEmployees", availableEmployees);
        model.addAttribute("assignedEmployees", assignedEmployees);

        return "employees/list";
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportToCsv() {
        List<Employee> employees = employeeRepository.findAll();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(osw)) {

            writer.println("ID,Name,Email,Department,Role,Skills,Available,Created At");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Employee emp : employees) {
                writer.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s,%s%n",
                        emp.getId(),
                        emp.getName(),
                        emp.getEmail(),
                        emp.getDepartment(),
                        emp.getRole() != null ? emp.getRole() : "",
                        emp.getSkills() != null ? String.join("; ", emp.getSkills()) : "",
                        emp.getAvailable() ? "Yes" : "No",
                        emp.getCreatedAt() != null ? emp.getCreatedAt().format(formatter) : ""
                );
            }

            writer.flush();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "employees_export.csv");

            return ResponseEntity.ok().headers(headers).body(baos.toByteArray());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(("Error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }
}