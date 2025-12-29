package com.workforce.workforceplanning.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "project_skill_requirements")
public class ProjectSkillRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String skill;

    @Column(nullable = false)
    private Integer requiredCount;

    // ===== Constructors =====

    public ProjectSkillRequirement() {}

    public ProjectSkillRequirement(Project project, String skill, Integer requiredCount) {
        this.project = project;
        this.skill = skill;
        this.requiredCount = requiredCount;
    }

    // ===== Getters & Setters =====

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public String getSkill() {
        return skill;
    }

    public Integer getRequiredCount() {
        return requiredCount;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public void setRequiredCount(Integer requiredCount) {
        this.requiredCount = requiredCount;
    }
}
