package com.workforce.workforceplanning.model;

public enum ProjectStatus {
    PENDING,           // Initial state, waiting for approval
    APPROVED,          // Approved and ready for staff assignment
    REJECTED,
    STAFFING,          // Resource Planner is assigning// Rejected
    IN_PROGRESS,       // Staff assigned and work started
    COMPLETED,         // Project finished
    CANCELLED          // Project cancelled
}