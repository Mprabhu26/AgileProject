package com.workforce.workforceplanning.dto;

public class TaskCompleteRequest {

    private boolean approved;
    private Long projectId;

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
}
