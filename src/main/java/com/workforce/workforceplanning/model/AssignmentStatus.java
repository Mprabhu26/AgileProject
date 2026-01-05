package com.workforce.workforceplanning.model;


public enum AssignmentStatus {
    PENDING,      // Awaiting approval
    ASSIGNED,      // Approved and active
    IN_PROGRESS,   // Work in progress
    COMPLETED,     // Assignment completed
    CANCELLED      // Assignment cancelled
}