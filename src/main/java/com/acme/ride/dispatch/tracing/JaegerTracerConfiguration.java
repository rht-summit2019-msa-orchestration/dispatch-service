package com.acme.ride.dispatch.tracing;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JaegerTracerConfiguration {

    Logger log = LoggerFactory.getLogger(JaegerTracerConfiguration.class);

    @Autowired
    private JaegerTracerProperties properties;

    @Bean
    public Tracer getTracer() {
        String serviceName = properties.getServiceName();
        if (serviceName == null || serviceName.isEmpty()) {
            log.info("No Service Name set. Skipping initialization of the Jaeger Tracer.");
            return GlobalTracer.get();
        }

        io.jaegertracing.Configuration configuration = new io.jaegertracing.Configuration(serviceName)
                .withSampler(new io.jaegertracing.Configuration.SamplerConfiguration()
                        .withType(properties.getSamplerType())
                        .withParam(properties.getSamplerParam())
                        .withManagerHostPort(properties.getSamplerManagerHostPort()))
                .withReporter(new io.jaegertracing.Configuration.ReporterConfiguration()
                        .withLogSpans(properties.isReporterLogSpans())
                        .withFlushInterval(properties.getReporterFlushInterval())
                        .withMaxQueueSize(properties.getReporterMaxQueueSize())
                        .withSender(new io.jaegertracing.Configuration.SenderConfiguration()
                                .withAgentHost(properties.getAgentHost())
                                .withAgentPort(properties.getAgentPort())));
        return configuration.getTracer();
    }
}
