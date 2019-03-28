package com.acme.ride.dispatch.tracing;

import java.util.Map;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.kafka.core.ConsumerFactory;

public class TracingKafkaConsumerFactory<K, V> implements ConsumerFactory<K, V> {

    private final ConsumerFactory<K, V> consumerFactory;
    private final Tracer tracer;

    public TracingKafkaConsumerFactory(ConsumerFactory<K, V> consumerFactory, Tracer tracer) {
        this.tracer = tracer;
        this.consumerFactory = consumerFactory;
    }

    public TracingKafkaConsumerFactory(ConsumerFactory<K, V> consumerFactory) {
        this(consumerFactory, GlobalTracer.get());
    }

    @Override
    public Consumer<K, V> createConsumer() {
        return new TracingKafkaConsumer<>(consumerFactory.createConsumer(), tracer);
    }

    @Override
    public Consumer<K, V> createConsumer(String clientIdSuffix) {
        return new TracingKafkaConsumer<>(consumerFactory.createConsumer(clientIdSuffix), tracer);
    }

    @Override
    public Consumer<K, V> createConsumer(String groupId, String clientIdSuffix) {
        return new TracingKafkaConsumer<>(consumerFactory.createConsumer(groupId, clientIdSuffix), tracer);
    }

    @Override
    public Consumer<K, V> createConsumer(String groupId, String clientIdPrefix, String clientIdSuffix) {
        return new TracingKafkaConsumer<>(consumerFactory.createConsumer(groupId, clientIdPrefix, clientIdSuffix), tracer);
    }

    @Override
    public boolean isAutoCommit() {
        return consumerFactory.isAutoCommit();
    }

    @Override
    public Map<String, Object> getConfigurationProperties() {
        return consumerFactory.getConfigurationProperties();
    }
}
