package com.workforce.workforceplanning.dto;

public class SkillRequirementDto {

    private String skill;
    private Integer requiredCount;  // ← Field name is requiredCount

    // Constructors
    public SkillRequirementDto() {
    }

    public SkillRequirementDto(String skill, Integer count) {
        this.skill = skill;
        this.requiredCount = count;  // ← Use requiredCount
    }

    // Getters and Setters
    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public Integer getRequiredCount() {
        return requiredCount;
    }

    public void setRequiredCount(Integer requiredCount) {
        this.requiredCount = requiredCount;
    }

    // ADD THIS ALIAS METHOD (for compatibility)
    public Integer getCount() {
        return requiredCount;
    }

    public void setCount(Integer count) {
        this.requiredCount = count;
    }
}