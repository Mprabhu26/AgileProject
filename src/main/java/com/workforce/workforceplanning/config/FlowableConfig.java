package com.workforce.workforceplanning.config;

import org.flowable.engine.impl.delegate.invocation.DefaultDelegateInterceptor;
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
        // For older Flowable versions, use a Map for beans
        Map<Object, Object> beans = new HashMap<>();

        // Manually register your delegate beans
        beans.put("projectStartDelegate",
                applicationContext.getBean("projectStartDelegate"));
        beans.put("projectApprovalDelegate",
                applicationContext.getBean("projectApprovalDelegate"));
        beans.put("assignmentDelegate",
                applicationContext.getBean("assignmentDelegate"));
        beans.put("projectCompletionDelegate",
                applicationContext.getBean("projectCompletionDelegate"));
        beans.put("notificationDelegate",
                applicationContext.getBean("notificationDelegate"));

        // Set the beans map
        configuration.setBeans(beans);

        // Basic configuration
        configuration.setDatabaseSchemaUpdate("true");
        configuration.setAsyncExecutorActivate(true);
    }
}