package com.workforce.workforceplanning.model;


public enum AssignmentStatus {
    PENDING("Pending", "bg-yellow-100 text-yellow-800"),
    ASSIGNED("Assigned", "bg-blue-100 text-blue-800"),
    APPROVED("Approved", "bg-green-100 text-green-800"),
    REJECTED("Rejected", "bg-red-100 text-red-800"),
    CONFIRMED("Confirmed", "bg-indigo-100 text-indigo-800"),
    IN_PROGRESS("In Progress", "bg-purple-100 text-purple-800"),
    COMPLETED("Completed", "bg-green-200 text-green-900"),
    CANCELLED("Cancelled", "bg-gray-100 text-gray-800");

    private final String displayName;
    private final String cssClasses;

    AssignmentStatus(String displayName, String cssClasses) {
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