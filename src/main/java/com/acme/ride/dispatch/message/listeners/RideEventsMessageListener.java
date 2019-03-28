package com.acme.ride.dispatch.message.listeners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;

import com.acme.ride.dispatch.dao.RideDao;
import com.acme.ride.dispatch.entity.Ride;
import com.acme.ride.dispatch.message.model.Message;
import com.acme.ride.dispatch.message.model.RideEndedEvent;
import com.acme.ride.dispatch.message.model.RideRequestedEvent;
import com.acme.ride.dispatch.message.model.RideStartedEvent;
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
import org.springframework.beans.factory.annotation.Value;
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
public class RideEventsMessageListener {

    private final static Logger log = LoggerFactory.getLogger(RideEventsMessageListener.class);

    private static final String TYPE_RIDE_REQUESTED_EVENT = "RideRequestedEvent";
    private static final String TYPE_RIDE_STARTED_EVENT = "RideStartedEvent";
    private static final String TYPE_RIDE_ENDED_EVENT = "RideEndedEvent";
    private static final String[] ACCEPTED_MESSAGE_TYPES = {TYPE_RIDE_REQUESTED_EVENT, TYPE_RIDE_STARTED_EVENT, TYPE_RIDE_ENDED_EVENT};

    @Autowired
    private ProcessService processService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RideDao rideDao;

    @Autowired
    private Tracer tracer;

    @Autowired
    private MeterRegistry meterRegistry;

    @Value("${dispatch.deployment.id}")
    private String deploymentId;

    @Value("${dispatch.process.id}")
    private String processId;

    @Value("${dispatch.assign.driver.expire.duration}")
    private String assignDriverExpireDuration;

    private CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

    private Map<String, Timer> timers = new HashMap<>();

    @PostConstruct
    public void initTimers() {
        timers.put(TYPE_RIDE_REQUESTED_EVENT, Timer.builder("dispatch-service.message.process").tags("type", TYPE_RIDE_REQUESTED_EVENT).register(meterRegistry));
        timers.put(TYPE_RIDE_STARTED_EVENT, Timer.builder("dispatch-service.message.process").tags("type", TYPE_RIDE_STARTED_EVENT).register(meterRegistry));
        timers.put(TYPE_RIDE_ENDED_EVENT, Timer.builder("dispatch-service.message.process").tags("type", TYPE_RIDE_ENDED_EVENT).register(meterRegistry));
    }

    @KafkaListener(topics = "${listener.destination.ride-event}")
    public void processMessage(@Payload String messageAsJson, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                               @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                               @Headers Map<String, Object> headers) {

        //create new span
        Scope scope = TracingKafkaUtils.buildChildSpan("processRideEventMessage", headers, tracer);

        try {
            checkMessageType(messageAsJson).ifPresent(m -> timedProcessMessage(m, messageAsJson, key, topic, partition));
        } finally {
            if (scope != null) {
                scope.close();
            }
        }
    }

    private void processRideRequestEvent(String messageAsJson, String key, String topic, int partition) {
        Message<RideRequestedEvent> message;
        try {

            message = new ObjectMapper().readValue(messageAsJson, new TypeReference<Message<RideRequestedEvent>>() {
            });

            String rideId = message.getPayload().getRideId();

            log.debug("Processing 'RideRequestedEvent' message for ride " + key + " from topic:partition " + topic + ":" + partition);

            Ride ride = new Ride();
            ride.setRideId(rideId);
            ride.setPassengerId(message.getPayload().getPassengerId());
            ride.setPickup(message.getPayload().getPickup());
            ride.setDestination(message.getPayload().getDestination());
            ride.setPrice(message.getPayload().getPrice());
            ride.setStatus(Ride.Status.REQUESTED);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("rideId", rideId);
            parameters.put("traceId", message.getTraceId());
            parameters.put("assign_driver_expire_duration", assignDriverExpireDuration);

            CorrelationKey correlationKey = correlationKeyFactory.newCorrelationKey(rideId);

            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.execute((TransactionStatus s) -> {
                rideDao.create(ride);
                Long pi = processService.startProcess(deploymentId, processId, correlationKey, parameters);
                log.debug("Started dispatch process for ride request " + rideId + ". ProcessInstanceId = " + pi);
                return null;
            });
        } catch (Exception e) {
            log.error("Error processing msg " + messageAsJson, e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void processRideStartedEvent(String messageAsJson, String key, String topic, int partition) {
        Message<RideStartedEvent> message;

        try {
            message = new ObjectMapper().readValue(messageAsJson, new TypeReference<Message<RideStartedEvent>>() {
            });

            String rideId = message.getPayload().getRideId();

            log.debug("Processing 'RideStartedEvent' message for ride " + key + " from topic:partition " + topic + ":" + partition);

            CorrelationKey correlationKey = correlationKeyFactory.newCorrelationKey(rideId);

            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.execute((TransactionStatus s) -> {
                ProcessInstance instance = processService.getProcessInstance(correlationKey);
                processService.signalProcessInstance(instance.getId(), "RideStarted", null);
                return null;
            });
        } catch (Exception e) {
            log.error("Error processing msg " + messageAsJson, e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void processRideEndedEvent(String messageAsJson, String key, String topic, int partition) {
        Message<RideEndedEvent> message;

        try {
            message = new ObjectMapper().readValue(messageAsJson, new TypeReference<Message<RideEndedEvent>>() {
            });

            String rideId = message.getPayload().getRideId();

            log.debug("Processing 'RideEndedEvent' message for ride " + key + " from topic:partition " + topic + ":" + partition);

            CorrelationKey correlationKey = correlationKeyFactory.newCorrelationKey(rideId);

            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.execute((TransactionStatus s) -> {
                ProcessInstance instance = processService.getProcessInstance(correlationKey);
                processService.signalProcessInstance(instance.getId(), "RideEnded", null);
                return null;
            });
        } catch (Exception e) {
            log.error("Error processing msg " + messageAsJson, e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private Optional<String> checkMessageType(String messageAsJson) {
        try {
            String messageType = JsonPath.read(messageAsJson, "$.messageType");
            if (Arrays.asList(ACCEPTED_MESSAGE_TYPES).contains(messageType)) {
                return Optional.of(messageType);
            }
            log.debug("Message with type '" + messageType + "' is ignored");
        } catch (Exception e) {
            log.warn("Unexpected message without 'messageType' field.");
        }
        Optional.ofNullable(tracer.activeSpan()).ifPresent(s -> new StringTag("msg.accepted").set(s, "false"));
        return Optional.empty();
    }

    private void timedProcessMessage(String messageType, String messageAsJson, String key, String topic, int partition) {

        Optional<Timer> timer = Optional.ofNullable(timers.get(messageType));
        long start = System.currentTimeMillis();
        try {
            switch (messageType) {
                case TYPE_RIDE_REQUESTED_EVENT:
                    processRideRequestEvent(messageAsJson, key, topic, partition);
                    break;
                case TYPE_RIDE_STARTED_EVENT:
                    processRideStartedEvent(messageAsJson, key, topic, partition);
                    break;
                case TYPE_RIDE_ENDED_EVENT:
                    processRideEndedEvent(messageAsJson, key, topic, partition);
                    break;
            }
        } finally {
            timer.ifPresent(t -> t.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS));
        }
    }
}