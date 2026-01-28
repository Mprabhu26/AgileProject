package com.workforce.workforceplanning.model;

public enum ProjectStatus {
    DRAFT,              // Project created but not published
    PENDING,            // Initial state, waiting for approval
    PUBLISHED,          // Published to employees (visible to all)
    PENDING_APPROVAL,   // Workflow started, awaiting dept head approval
    APPROVED,           // Approved and ready for staff assignment
    REJECTED,           // Rejected by department head
    STAFFING,           // Resource Planner is assigning staff
    ASSIGNED,           // Staff has been assigned
    IN_PROGRESS,        // Staff assigned and work started
    COMPLETED,          // Project finished
    CANCELLED,          // Project cancelled
    ACTIVE,
    STARTED
}