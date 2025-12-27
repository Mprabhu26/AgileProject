package com.workforce.workforceplanning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.workforce.workforceplanning")
@EnableJpaRepositories(basePackages = "com.workforce.workforceplanning.repository")
@EntityScan(basePackages = "com.workforce.workforceplanning.model")
public class WorkforcePlanningApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkforcePlanningApplication.class, args);
    }
}
