package com.workforce.workforceplanning.dto;

public class SkillRequirementDto {

    private String skill;
    private Integer count;

    // Constructors
    public SkillRequirementDto() {
    }

    public SkillRequirementDto(String skill, Integer count) {
        this.skill = skill;
        this.count = count;
    }

    // Getters and Setters
    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}