package com.workforce.workforceplanning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.workforce.workforceplanning"  // Scans ALL sub-packages
})
public class WorkforcePlanningApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkforcePlanningApplication.class, args);
    }
}