package com.workforce.workforceplanning.service;

import com.workforce.workforceplanning.model.Assignment;
import com.workforce.workforceplanning.repository.AssignmentRepository;
import org.springframework.stereotype.Service;

@Service
public class AssignmentService {

    private final AssignmentRepository repository;

    public AssignmentService(AssignmentRepository repository) {
        this.repository = repository;
    }

    public Assignment save(Assignment assignment) {
        return repository.save(assignment);
    }
}
