package com.workforce.workforceplanning.dto;

public class SkillRequirementDto {
    private String skill;
    private Integer requiredCount;  // Keep field name as requiredCount

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    // ✅ Change from getRequiredCount() to getCount() to match controller calls
    public Integer getCount() {
        return requiredCount;
    }

    // ✅ Change from setRequiredCount() to setCount()
    public void setCount(Integer count) {
        this.requiredCount = count;
    }

    // Optional: Keep getRequiredCount() for clarity
    public Integer getRequiredCount() {
        return requiredCount;
    }
}