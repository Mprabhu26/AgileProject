package com.workforce.workforceplanning.dto;

public class SkillRequirementDto {
    
    private String skill;
    private Integer requiredCount;
    
    // Constructors
    public SkillRequirementDto() {
    }
    
    public SkillRequirementDto(String skill, Integer count) {
        this.skill = skill;
        this.requiredCount = count;
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
    
    // Alias methods for compatibility
    public Integer getCount() {
        return requiredCount;
    }
    
    public void setCount(Integer count) {
        this.requiredCount = count;
    }
}