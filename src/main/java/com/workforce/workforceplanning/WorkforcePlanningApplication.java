package com.workforce.workforceplanning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.workforce.workforceplanning")
public class WorkforcePlanningApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkforcePlanningApplication.class, args);
    }
}