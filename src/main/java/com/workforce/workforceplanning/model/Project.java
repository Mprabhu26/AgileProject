package com.workforce.workforceplanning.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.workforce.workforceplanning.model.ProjectSkillRequirement;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.PENDING;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    private BigDecimal budget;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "total_employees_required", nullable = false)
    private Integer totalEmployeesRequired;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 🔗 Project → Skill Requirements
    @OneToMany(
            mappedBy = "project",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonManagedReference
    private List<ProjectSkillRequirement> skillRequirements = new ArrayList<>();

    // ===== Constructors =====
    public Project() {}

    // ===== Lifecycle Callback =====
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ===== Getters & Setters =====
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getTotalEmployeesRequired() {
        return totalEmployeesRequired;
    }

    public void setTotalEmployeesRequired(Integer totalEmployeesRequired) {
        this.totalEmployeesRequired = totalEmployeesRequired;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ProjectSkillRequirement> getSkillRequirements() {
        return skillRequirements;
    }

    public void setSkillRequirements(List<ProjectSkillRequirement> skillRequirements) {
        this.skillRequirements = skillRequirements;
    }

    // ===== Helper Methods =====
    public void addSkillRequirement(ProjectSkillRequirement requirement) {
        skillRequirements.add(requirement);
        requirement.setProject(this);
    }

    public void removeSkillRequirement(ProjectSkillRequirement requirement) {
        skillRequirements.remove(requirement);
        requirement.setProject(null);
    }

    @Override
    public String toString() {
        return "Project{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", createdBy='" + createdBy + '\'' +
                ", totalEmployeesRequired=" + totalEmployeesRequired +
                '}';
    }
}