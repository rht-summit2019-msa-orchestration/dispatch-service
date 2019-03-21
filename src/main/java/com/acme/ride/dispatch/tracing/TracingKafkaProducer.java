package com.acme.ride.dispatch.tracing;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.ProducerFencedException;

public class TracingKafkaProducer<K, V> implements Producer<K, V> {

    private Producer<K, V> producer;
    private final Tracer tracer;

    public TracingKafkaProducer(Producer<K, V> producer, Tracer tracer) {
        this.producer = producer;
        this.tracer = tracer;
    }

    public TracingKafkaProducer(Producer<K, V> producer) {
        this.producer = producer;
        this.tracer = GlobalTracer.get();
    }

    @Override
    public void initTransactions() {
        producer.initTransactions();
    }

    @Override
    public void beginTransaction() throws ProducerFencedException {
        producer.beginTransaction();
    }

    @Override
    public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> map, String s) throws ProducerFencedException {
        producer.sendOffsetsToTransaction(map, s);
    }

    @Override
    public void commitTransaction() throws ProducerFencedException {
        producer.commitTransaction();
    }

    @Override
    public void abortTransaction() throws ProducerFencedException {
        producer.abortTransaction();
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> producerRecord) {
        return send(producerRecord, null);
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> producerRecord, Callback callback) {
        try (Scope scope = TracingKafkaUtils
                .buildAndInjectSpan(producerRecord, tracer)) {
            Callback wrappedCallback = new TracingCallback(callback, scope.span(), tracer);
            return producer.send(producerRecord, wrappedCallback);
        }
    }

    @Override
    public void flush() {
        producer.flush();
    }

    @Override
    public List<PartitionInfo> partitionsFor(String s) {
        return producer.partitionsFor(s);
    }

    @Override
    public Map<MetricName, ? extends Metric> metrics() {
        return producer.metrics();
    }

    @Override
    public void close() {
        producer.close();
    }

    @Override
    public void close(long l, TimeUnit timeUnit) {
        producer.close(l, timeUnit);
    }
}
