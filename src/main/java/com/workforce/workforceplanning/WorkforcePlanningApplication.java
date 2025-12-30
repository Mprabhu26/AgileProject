package com.workforce.workforceplanning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;

@SpringBootApplication
@ComponentScan(basePackages = "com.workforce.workforceplanning")
@EnableJpaRepositories(basePackages = "com.workforce.workforceplanning.repository")
@EntityScan(basePackages = "com.workforce.workforceplanning.model")
public class WorkforcePlanningApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkforcePlanningApplication.class, args);
    }

    @Bean
    public Java8TimeDialect java8TimeDialect() {
        return new Java8TimeDialect();
    }
}
