package com.workforce.workforceplanning.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CreateProjectForm {
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private Integer totalEmployeesRequired;
    private List<SkillRequirementDto> skillRequirements;
    private String status;

    // ✅ ADD ALL GETTERS AND SETTERS
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

    public Integer getTotalEmployeesRequired() {
        return totalEmployeesRequired;
    }

    public void setTotalEmployeesRequired(Integer totalEmployeesRequired) {
        this.totalEmployeesRequired = totalEmployeesRequired;
    }

    public List<SkillRequirementDto> getSkillRequirements() {
        return skillRequirements;
    }

    public void setSkillRequirements(List<SkillRequirementDto> skillRequirements) {
        this.skillRequirements = skillRequirements;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}