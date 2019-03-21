package com.acme.ride.dispatch.tracing;

import java.util.Map;

import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TracingKafkaUtils {

    private static final Logger logger = LoggerFactory.getLogger(TracingKafkaUtils.class);

    public static final String TO_PREFIX = "To_";
    public static final String FROM_PREFIX = "From_";
    public static final String COMPONENT = "dispatch-service";

    static SpanContext extract(Headers headers, Tracer tracer) {

        SpanContext spanContext = tracer
                .extract(Format.Builtin.TEXT_MAP, new KafkaHeadersExtractAdapter(headers, false));

        if (spanContext != null) {
            return spanContext;
        }

        Span span = tracer.activeSpan();
        if (span != null) {
            return span.context();
        }
        return null;
    }

    public static SpanContext extractSpanContext(Headers headers, Tracer tracer) {
        return tracer
                .extract(Format.Builtin.TEXT_MAP, new KafkaHeadersExtractAdapter(headers, true));
    }

    public static SpanContext extractSpanContext(Map<String, Object> headers, Tracer tracer) {
        return tracer
                .extract(Format.Builtin.TEXT_MAP, new HeadersMapExtractAdapter(headers, true));
    }

    static <K,V> void buildAndFinishChildSpan(ConsumerRecord<K, V> record, Tracer tracer) {
        SpanContext parentContext = TracingKafkaUtils.extract(record.headers(), tracer);

        if (parentContext != null) {

            String consumerOper = FROM_PREFIX + record.topic(); // <====== It provides better readability in the UI
            Tracer.SpanBuilder spanBuilder = tracer.buildSpan(consumerOper)
                    .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CONSUMER);

            spanBuilder.addReference(References.FOLLOWS_FROM, parentContext);

            Span span = spanBuilder.start();
            SpanDecorator.onResponse(record, span);
            span.finish();

            // Inject created span context into record headers for extraction by client to continue span chain
            TracingKafkaUtils.injectSecond(span.context(), record.headers(), tracer);
        }
    }

    static void inject(SpanContext spanContext, Headers headers,
                       Tracer tracer) {
        tracer.inject(spanContext, Format.Builtin.TEXT_MAP,
                new KafkaHeadersInjectAdapter(headers, false));
    }

    static void injectSecond(SpanContext spanContext, Headers headers,
                             Tracer tracer) {
        tracer.inject(spanContext, Format.Builtin.TEXT_MAP,
                new KafkaHeadersInjectAdapter(headers, true));
    }

    static <K,V> Scope buildAndInjectSpan(ProducerRecord<K, V> record, Tracer tracer) {

        String producerOper = TO_PREFIX + record.topic(); // <======== It provides better readability in the UI
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(producerOper)
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_PRODUCER);

        SpanContext spanContext = TracingKafkaUtils.extract(record.headers(), tracer);

        if (spanContext != null) {
            spanBuilder.asChildOf(spanContext);
        }

        Scope scope = spanBuilder.startActive(false);
        SpanDecorator.onSend(record, scope.span());

        try {
            TracingKafkaUtils.inject(scope.span().context(), record.headers(), tracer);
        } catch (Exception e) {
            // it can happen if headers are read only (when record is sent second time)
            logger.error("failed to inject span context. sending record second time?", e);
        }

        return scope;
    }

    public static Scope buildChildSpan(String operationName, Map<String, Object> headers, Tracer tracer) {

        SpanContext spanContext = extractSpanContext(headers, tracer);

        if (spanContext != null) {
            return spanBuilder(operationName, tracer).addReference(References.CHILD_OF, spanContext)
                    .startActive(true);
        }
        return null;
    }

    private static Tracer.SpanBuilder spanBuilder(String operationName, Tracer tracer) {
        return  tracer.buildSpan(operationName).ignoreActiveSpan()
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CONSUMER)
                .withTag(Tags.COMPONENT.getKey(), COMPONENT);
    }
}
