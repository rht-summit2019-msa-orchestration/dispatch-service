package com.acme.ride.dispatch.message.listeners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.acme.ride.dispatch.dao.RideDao;
import com.acme.ride.dispatch.entity.Ride;
import com.acme.ride.dispatch.message.model.Message;
import com.acme.ride.dispatch.message.model.RideRequestedEvent;
import com.acme.ride.dispatch.message.model.RideEndedEvent;
import com.acme.ride.dispatch.message.model.RideStartedEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
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

    @Value("${dispatch.deployment.id}")
    private String deploymentId;

    @Value("${dispatch.process.id}")
    private String processId;

    @Value("${dispatch.assign.driver.expire.duration}")
    private String assignDriverExpireDuration;

    private CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

    @KafkaListener(topics = "${listener.destination.ride-event}")
    public void processMessage(@Payload String messageAsJson, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                               @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition) {

        String messageType = getMessageType(messageAsJson);

        if (messageType.isEmpty() || !accept(messageType)) {
            return;
        }

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
    }

    private void processRideRequestEvent(String messageAsJson, String key, String topic, int partition) {
        Message<RideRequestedEvent> message;
        try {

            message = new ObjectMapper().readValue(messageAsJson, new TypeReference<Message<RideRequestedEvent>>() {});

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
            message = new ObjectMapper().readValue(messageAsJson, new TypeReference<Message<RideStartedEvent>>() {});

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
            message = new ObjectMapper().readValue(messageAsJson, new TypeReference<Message<RideEndedEvent>>() {});

            String rideId = message.getPayload().getRideId();

            log.debug("Processing 'RideEndedEvent' message for ride "+ key + " from topic:partition " + topic + ":" + partition);

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

    private boolean accept(String messageType) {
        if (!Arrays.stream(ACCEPTED_MESSAGE_TYPES).anyMatch(messageType::equals)) {
            log.debug("Message with type '" + messageType + "' is ignored");
            return false;
        }
        return true;
    }

    private String getMessageType(String messageAsJson) {
        try {
            return JsonPath.read(messageAsJson, "$.messageType");
        } catch (Exception e) {
            log.warn("Unexpected message without 'messageType' field.");
            return "";
        }
    }
}
