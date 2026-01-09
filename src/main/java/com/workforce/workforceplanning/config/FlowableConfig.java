package com.workforce.workforceplanning.config;

import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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

        // Register ALL delegate beans
        registerDelegate(beans, "projectStartDelegate");
        registerDelegate(beans, "projectApprovalDelegate");
        registerDelegate(beans, "assignmentDelegate");
        registerDelegate(beans, "notificationDelegate");
        registerDelegate(beans, "projectCompletionDelegate");

        // Set the beans map
        configuration.setBeans(beans);

        System.out.println("✅ FlowableConfig: Registered " + beans.size() + " delegate beans");
    }

    private void registerDelegate(Map<Object, Object> beans, String beanName) {
        try {
            Object delegate = applicationContext.getBean(beanName);
            beans.put(beanName, delegate);
            System.out.println("✅ FlowableConfig: Registered " + beanName);
        } catch (Exception e) {
            System.err.println("❌ FlowableConfig: Failed to register " + beanName + ": " + e.getMessage());
        }
    }
}