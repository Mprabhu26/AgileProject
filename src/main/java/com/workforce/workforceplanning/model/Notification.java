package com.workforce.workforceplanning.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class  Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id")  // REMOVED: nullable = false
    private Long employeeId;  // For employee notifications (can be null)

    @Column(name = "username")
    private String username;  // For PM notifications by username (can be null)

    private String title;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "related_assignment_id")
    private Long relatedAssignmentId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "project_name")
    private String projectName;  // Store project name for display

    // ===== CONSTRUCTORS =====
    // Constructor 1: Default
    public Notification() {
        this.createdAt = LocalDateTime.now();
    }

    // Constructor 2: For employee notifications (employeeId is Long)
    public Notification(Long employeeId, String title, String message, NotificationType type) {
        this.employeeId = employeeId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    // Constructor 3: For PM notifications (username is String)
    public Notification(String username, String title, String message, NotificationType type) {
        this.username = username;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters remain the same...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getRelatedAssignmentId() { return relatedAssignmentId; }
    public void setRelatedAssignmentId(Long relatedAssignmentId) { this.relatedAssignmentId = relatedAssignmentId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
}