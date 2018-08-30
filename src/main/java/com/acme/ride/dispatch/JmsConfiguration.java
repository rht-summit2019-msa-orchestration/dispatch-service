package com.acme.ride.dispatch;

import javax.jms.ConnectionFactory;

import org.amqphub.spring.boot.jms.autoconfigure.AMQP10JMSProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;

@Configuration
@ConfigurationProperties("spring.jms")
public class JmsConfiguration {

    @Autowired
    private AMQP10JMSProperties amqJmsProperties;

    @Autowired
    private JmsProperties jmsProperties;

    private boolean subscriptionShared;

    private boolean subscriptionDurable;

    private boolean transacted;

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            DefaultJmsListenerContainerFactoryConfigurer configurer,
            ConnectionFactory listenerConnectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setSubscriptionShared(subscriptionShared);
        factory.setSubscriptionDurable(subscriptionDurable);
        configurer.configure(factory, listenerConnectionFactory);
        // override
        factory.setTransactionManager(null);
        factory.setSessionTransacted(true);
        return factory;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setPubSubDomain(this.jmsProperties.isPubSubDomain());
        jmsTemplate.setSessionTransacted(transacted);
        return jmsTemplate;
    }

    public void setSubscriptionShared(boolean subscriptionShared) {
        this.subscriptionShared = subscriptionShared;
    }

    public void setSubscriptionDurable(boolean subscriptionDurable) {
        this.subscriptionDurable = subscriptionDurable;
    }

    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }

}
