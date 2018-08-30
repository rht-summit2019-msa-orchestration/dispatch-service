package com.acme.ride.dispatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jms.annotation.EnableJms;

@SpringBootConfiguration
@ComponentScan
@EnableAutoConfiguration
@EnableJms
public class DispatchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DispatchServiceApplication.class, args);
    }

}
