package com.acme.ride.dispatch.tracing;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.kafka.core.ProducerFactory;

public class TracingKafkaProducerFactory<K, V> implements ProducerFactory<K, V>, DisposableBean {

    private final ProducerFactory<K, V> producerFactory;
    private final Tracer tracer;

    public TracingKafkaProducerFactory(ProducerFactory<K, V> producerFactory, Tracer tracer) {
        this.producerFactory = producerFactory;
        this.tracer = tracer;
    }

    public TracingKafkaProducerFactory(ProducerFactory<K,V> producerFactory) {
        this.producerFactory = producerFactory;
        this.tracer = GlobalTracer.get();
    }


    @Override
    public void destroy() throws Exception {
        if (producerFactory instanceof DisposableBean) {
            ((DisposableBean) producerFactory).destroy();
        }
    }

    @Override
    public Producer<K, V> createProducer() {
        return new TracingKafkaProducer<>(producerFactory.createProducer(), tracer);
    }

    @Override
    public boolean transactionCapable() {
        return producerFactory.transactionCapable();
    }
}
