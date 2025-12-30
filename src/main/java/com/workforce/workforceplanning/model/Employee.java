package com.workforce.workforceplanning.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(name = "name", nullable = false)
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "employee_skills",
            joinColumns = @JoinColumn(name = "employee_id", referencedColumnName = "id")
    )
    @Column(name = "skill")
    private Set<String> skills = new HashSet<>();

    @Column(name = "department", nullable = false)
    private String department;

    @Column(name = "available", nullable = false)
    private Boolean available = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        // Ensure available is never null
        if (this.available == null) {
            this.available = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Constructors
    public Employee() {
        // available already defaults to true
    }

    public Employee(String name, String email, Set<String> skills, String department) {
        this.name = name;
        this.email = email;
        this.skills = skills != null ? skills : new HashSet<>();
        this.department = department;
        // available defaults to true
    }

    public Employee(String name, String email, Set<String> skills, String department, Boolean available) {
        this(name, email, skills, department);
        this.available = available != null ? available : true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<String> getSkills() {
        // Return defensive copy or unmodifiable set
        return new HashSet<>(skills);
    }

    public void setSkills(Set<String> skills) {
        this.skills = skills != null ? new HashSet<>(skills) : new HashSet<>();
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available != null ? available : true;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public void addSkill(String skill) {
        if (skill != null && !skill.trim().isEmpty()) {
            this.skills.add(skill.trim());
        }
    }

    public void addSkills(Set<String> newSkills) {
        if (newSkills != null) {
            for (String skill : newSkills) {
                addSkill(skill);
            }
        }
    }

    public void removeSkill(String skill) {
        if (skill != null && this.skills != null) {
            this.skills.remove(skill);
        }
    }

    public boolean hasSkill(String skill) {
        return skill != null && this.skills != null && this.skills.contains(skill);
    }

    // Business logic method
    public boolean isAvailableForProject() {
        return Boolean.TRUE.equals(available);
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", skills=" + skills +
                ", department='" + department + '\'' +
                ", available=" + available +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    // Equals and hashCode (important for collections and JPA)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Employee employee = (Employee) o;

        // Use email for equality since it's unique
        return email != null ? email.equals(employee.email) : employee.email == null;
    }

    @Override
    public int hashCode() {
        return email != null ? email.hashCode() : 0;
    }
}