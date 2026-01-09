package com.workforce.workforceplanning.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "assignments")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 🔗 MANY ASSIGNMENTS → ONE PROJECT
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // 🔗 MANY ASSIGNMENTS → ONE EMPLOYEE (THIS WAS MISSING!)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AssignmentStatus status;

    @Column(name = "assigned_at")
    private java.time.LocalDateTime assignedAt;

    // ===== Constructors =====
    public Assignment() {
    }

    public Assignment(Project project, Employee employee, AssignmentStatus status) {
        this.project = project;
        this.employee = employee;
        this.status = status;
        this.assignedAt = java.time.LocalDateTime.now();
    }

    // ===== Getters & Setters =====
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }

    public java.time.LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(java.time.LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) {
            assignedAt = java.time.LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "Assignment{" +
                "id=" + id +
                ", project=" + (project != null ? project.getName() : "null") +
                ", employee=" + (employee != null ? employee.getName() : "null") +
                ", status=" + status +
                ", assignedAt=" + assignedAt +
                '}';
    }
}