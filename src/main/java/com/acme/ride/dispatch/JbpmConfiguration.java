package com.acme.ride.dispatch;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManagerFactory;

import com.acme.ride.dispatch.message.model.AssignDriverCommand;
import com.acme.ride.dispatch.wih.MessageSenderWorkItemHandler;
import org.drools.persistence.api.TransactionManager;
import org.jbpm.executor.ExecutorServiceFactory;
import org.jbpm.executor.impl.event.ExecutorEventSupportImpl;
import org.jbpm.shared.services.impl.TransactionalCommandService;
import org.kie.api.executor.ExecutorService;
import org.kie.api.runtime.manager.RegisterableItemsFactory;
import org.kie.api.runtime.manager.RuntimeEnvironment;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.task.UserGroupCallback;
import org.kie.spring.factorybeans.RuntimeEnvironmentFactoryBean;
import org.kie.spring.factorybeans.RuntimeManagerFactoryBean;
import org.kie.spring.jbpm.services.SpringTransactionalCommandService;
import org.kie.spring.persistence.KieSpringTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
public class JbpmConfiguration {

    @Autowired
    private JbpmProperties properties;

    @Value("${dispatch.process.kbase}")
    private String dispatchProcessKbase;

    @Value("${dispatch.process.ksession}")
    private String dispatchProcessKsession;

    @Autowired
    private MessageSenderWorkItemHandler messageSenderWorkItemHandler;

    public UserGroupCallback userGroupCallback() {
        return new SimpleUserGroupCallback();
    }

    public RegisterableItemsFactory registerableItemsFactory() {
        ByInstanceRegisterableItemsFactory registerableItemsFactory = new ByInstanceRegisterableItemsFactory();
        messageSenderWorkItemHandler.addPayloadBuilder("AssignDriverCommand", AssignDriverCommand::build);
        registerableItemsFactory.addWorkItemHandler("SendMessage", messageSenderWorkItemHandler);
        return registerableItemsFactory;
    }

    @Bean
    public TransactionManager kieTransactionManager(JtaTransactionManager transactionManager) {
        return new KieSpringTransactionManager(transactionManager);
    }

    @Bean
    public TransactionalCommandService transactionalCommandService(EntityManagerFactory entityManagerFactory, TransactionManager kieTransactionManager,
                JtaTransactionManager transactionManager) {
        return new SpringTransactionalCommandService(entityManagerFactory, kieTransactionManager, transactionManager);
    }

    public ExecutorService executorService(EntityManagerFactory entityManagerFactory, TransactionalCommandService transactionalCommandService) {

        if (!properties.getExecutor().isEnabled()) {
            return null;
        }

        //disable JMS
        System.setProperty("org.kie.executor.jms","false");

        ExecutorEventSupportImpl eventSupport = new ExecutorEventSupportImpl();

        // configure services
        ExecutorService service = ExecutorServiceFactory.newExecutorService(entityManagerFactory, transactionalCommandService, eventSupport);

        service.setInterval(properties.getExecutor().getInterval());
        service.setRetries(properties.getExecutor().getRetries());
        service.setThreadPoolSize(properties.getExecutor().getThreadPoolSize());
        service.setTimeunit(TimeUnit.valueOf(properties.getExecutor().getTimeUnit()));

        service.init();

        return service;
    }

    public RuntimeEnvironment runtimeEnvironment(EntityManagerFactory entityManagerFactory,
                                                 JtaTransactionManager transactionManager, TransactionalCommandService transactionalCommandService) throws Exception {
        RuntimeEnvironmentFactoryBean runtimeEnvironmentFactoryBean = new RuntimeEnvironmentFactoryBean();
        runtimeEnvironmentFactoryBean.setType(RuntimeEnvironmentFactoryBean.TYPE_DEFAULT_KJAR_CL);
        runtimeEnvironmentFactoryBean.setKbaseName(dispatchProcessKbase);
        runtimeEnvironmentFactoryBean.setKsessionName(dispatchProcessKsession);
        runtimeEnvironmentFactoryBean.setEntityManagerFactory(entityManagerFactory);
        runtimeEnvironmentFactoryBean.setTransactionManager(transactionManager);
        runtimeEnvironmentFactoryBean.setUserGroupCallback(userGroupCallback());
        runtimeEnvironmentFactoryBean.setRegisterableItemsFactory(registerableItemsFactory());
        Map<String, Object> environmentEntries = new HashMap<>();
        environmentEntries.put("ExecutorService", executorService(entityManagerFactory, transactionalCommandService));
        runtimeEnvironmentFactoryBean.setEnvironmentEntries(environmentEntries);
        return (RuntimeEnvironment) runtimeEnvironmentFactoryBean.getObject();
    }

    @Bean(name = "runtimeManager")
    @DependsOn("springContext")
    public RuntimeManager runtimeManager(EntityManagerFactory entityManagerFactory, JtaTransactionManager transactionManager,
                                         TransactionalCommandService transactionalCommandService) throws Exception {
        RuntimeManagerFactoryBean runtimeManagerFactoryBean = new RuntimeManagerFactoryBean();
        runtimeManagerFactoryBean.setIdentifier("spring-rm");
        runtimeManagerFactoryBean.setRuntimeEnvironment(runtimeEnvironment(entityManagerFactory,
                transactionManager, transactionalCommandService));
        runtimeManagerFactoryBean.setType("PER_PROCESS_INSTANCE");
        return (RuntimeManager) runtimeManagerFactoryBean.getObject();
    }

}
