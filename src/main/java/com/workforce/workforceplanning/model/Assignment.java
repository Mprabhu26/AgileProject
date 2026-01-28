package com.workforce.workforceplanning.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;

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

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by")
    private String rejectedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

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

    // Getters and setters
    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    public String getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(String rejectedBy) { this.rejectedBy = rejectedBy; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

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