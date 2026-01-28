package com.workforce.workforceplanning.model;

public enum ApplicationStatus {
    PENDING("Pending", "bg-yellow-100 text-yellow-800"),
    APPROVED("Approved", "bg-green-100 text-green-800"),
    REJECTED("Rejected", "bg-red-100 text-red-800"),
    WITHDRAWN("Withdrawn", "bg-gray-100 text-gray-800"),
    CANCELLED("CANCELLED", "bg-gray-100 text-gray-800");

    private final String displayName;
    private final String cssClasses;

    ApplicationStatus(String displayName, String cssClasses) {
        this.displayName = displayName;
        this.cssClasses = cssClasses;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssClasses() {
        return cssClasses;
    }
}