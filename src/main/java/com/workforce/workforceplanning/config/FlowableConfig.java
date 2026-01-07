package com.workforce.workforceplanning.config;

import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class FlowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration>, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void configure(SpringProcessEngineConfiguration configuration) {
        // Create a beans map for Flowable
        Map<Object, Object> beans = new HashMap<>();

        // MANUALLY register your delegate beans
        try {
            // Get the delegate bean by name
            Object projectApprovalDelegate = applicationContext.getBean("projectApprovalDelegate");
            beans.put("projectApprovalDelegate", projectApprovalDelegate);
            System.out.println("✅ FlowableConfig: Registered projectApprovalDelegate bean");
            System.out.println("   Bean class: " + projectApprovalDelegate.getClass().getName());

            // Check if dependencies are injected
            if (projectApprovalDelegate instanceof com.workforce.workforceplanning.workflow.ProjectApprovalDelegate) {
                var delegate = (com.workforce.workforceplanning.workflow.ProjectApprovalDelegate) projectApprovalDelegate;
                // You can't check private fields here, but logging will show in delegate constructor
            }

        } catch (Exception e) {
            System.err.println("❌ FlowableConfig: Failed to get projectApprovalDelegate bean: " + e.getMessage());
        }

        // Set the beans map
        configuration.setBeans(beans);

        // Enable Spring dependency injection for delegates
        configuration.setEnableProcessDefinitionInfoCache(true);

        System.out.println("✅ FlowableConfig: Configuration completed");
    }
}