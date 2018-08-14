package com.acme.ride.dispatch;

import javax.jms.ConnectionFactory;

import org.amqphub.spring.boot.jms.autoconfigure.AMQP10JMSConnectionFactoryFactory;
import org.amqphub.spring.boot.jms.autoconfigure.AMQP10JMSProperties;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

@Configuration
@ConfigurationProperties("spring.jms")
public class JmsConfiguration {

    @Autowired
    private AMQP10JMSProperties amqJmsProperties;

    @Autowired
    private JmsProperties jmsProperties;

    private int sessionCacheSize;

    private boolean subscriptionShared;

    private boolean subscriptionDurable;

    private boolean transacted;

    @Bean
    public ConnectionFactory connectionFactory() {
        JmsConnectionFactory jcf = new AMQP10JMSConnectionFactoryFactory(amqJmsProperties)
                .createConnectionFactory(JmsConnectionFactory.class);
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(jcf);
        connectionFactory.setSessionCacheSize(sessionCacheSize);
        return connectionFactory;
    }

    @Bean
    public ConnectionFactory listenerConnectionFactory() {
        JmsConnectionFactory jcf = new AMQP10JMSConnectionFactoryFactory(amqJmsProperties)
                .createConnectionFactory(JmsConnectionFactory.class);
        SingleConnectionFactory connectionFactory = new SingleConnectionFactory(jcf);
        return connectionFactory;
    }

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

    public void setSessionCacheSize(int sessionCacheSize) {
        this.sessionCacheSize = sessionCacheSize;
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
