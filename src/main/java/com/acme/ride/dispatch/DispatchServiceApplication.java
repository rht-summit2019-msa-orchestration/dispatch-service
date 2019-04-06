package com.acme.ride.dispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;

@SpringBootConfiguration
@ComponentScan
@EnableAutoConfiguration(exclude = { KafkaAutoConfiguration.class })
public class DispatchServiceApplication {

    private final static Logger log = LoggerFactory.getLogger(DispatchServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(DispatchServiceApplication.class);
        application.setRegisterShutdownHook(false);
        ConfigurableApplicationContext context = application.run(args);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("shutdownhook called");
            context.close();
        }));
    }

    @Bean
    CommandLineRunner startKafkaListeners() {
        return new CommandLineRunner() {

            @Autowired
            private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

            @Override
            public void run(String... strings) throws Exception {
                kafkaListenerEndpointRegistry.start();
            }
        };
    }

}
