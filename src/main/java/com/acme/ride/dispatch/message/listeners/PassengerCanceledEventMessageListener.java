package com.acme.ride.dispatch.message.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;

import com.acme.ride.dispatch.message.model.Message;
import com.acme.ride.dispatch.message.model.PassengerCanceledEvent;
import com.acme.ride.dispatch.tracing.TracingKafkaUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.tag.StringTag;
import org.jbpm.services.api.ProcessService;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.KieInternalServices;
import org.kie.internal.process.CorrelationKey;
import org.kie.internal.process.CorrelationKeyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class PassengerCanceledEventMessageListener {

    private final static Logger log = LoggerFactory.getLogger(PassengerCanceledEventMessageListener.class);

    private final static String TYPE_PASSENGER_CANCELED_EVENT = "PassengerCanceledEvent";

    @Autowired
    private ProcessService processService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Tracer tracer;

    @Autowired
    private MeterRegistry meterRegistry;

    private Map<String, Timer> timers = new HashMap<>();

    private CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

    @KafkaListener(topics = "${listener.destination.passenger-canceled-event}")
    public void processMessage(@Payload String messageAsJson, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                               @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                               @Headers Map<String, Object> headers) {

        //create new span
        Scope scope = TracingKafkaUtils.buildChildSpan("processPassengerCanceledMessage", headers, tracer);

        try {
            accept(messageAsJson).ifPresent(messageType -> {
                log.debug("Processing '" + messageType + "' message for ride " + key + " from topic:partition " + topic + ":" + partition);
                timedProcessMessage(messageType, messageAsJson);
            });

        } finally {

            if (scope != null) {
                scope.close();
            }
        }
    }

    private void processPassengerCanceled(String messageAsJson) {
        try {

            Message<PassengerCanceledEvent> message = new ObjectMapper().readValue(messageAsJson,
                    new TypeReference<Message<PassengerCanceledEvent>>() {});

            String rideId = message.getPayload().getRideId();

            CorrelationKey correlationKey = correlationKeyFactory.newCorrelationKey(rideId);

            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.execute((TransactionStatus s) -> {
                ProcessInstance instance = processService.getProcessInstance(correlationKey);
                processService.signalProcessInstance(instance.getId(), "PassengerCanceled", null);
                return null;
            });
        } catch (Exception e) {
            log.error("Error processing msg " + messageAsJson, e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private Optional<String> accept(String messageAsJson) {
        try {
            String messageType = JsonPath.read(messageAsJson, "$.messageType");
            if ("PassengerCanceledEvent".equalsIgnoreCase(messageType) ) {
                return Optional.of(messageType);
            } else {
                log.debug("Message with type '" + messageType + "' is ignored");
            }
        } catch (Exception e) {
            log.warn("Unexpected message without 'messageType' field.");
        }
        Optional.ofNullable(tracer.activeSpan()).ifPresent(s -> new StringTag("msg.accepted").set(s, "false"));
        return Optional.empty();
    }

    @PostConstruct
    public void initTimers() {
        timers.put(TYPE_PASSENGER_CANCELED_EVENT, Timer.builder("dispatch-service.message.process").tags("type",TYPE_PASSENGER_CANCELED_EVENT).register(meterRegistry));
    }

    private void timedProcessMessage(String messageType, String messageAsJson) {
        Optional<Timer> timer = Optional.ofNullable(timers.get(messageType));
        long start = System.currentTimeMillis();
        try {
            processPassengerCanceled(messageAsJson);
        } finally {
            timer.ifPresent(t -> t.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS));
        }
    }
}
