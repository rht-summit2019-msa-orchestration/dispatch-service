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
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.KieInternalServices;
import org.kie.internal.process.CorrelationAwareProcessRuntime;
import org.kie.internal.process.CorrelationKey;
import org.kie.internal.process.CorrelationKeyFactory;
import org.kie.internal.runtime.manager.context.CorrelationKeyContext;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
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
    private RuntimeManager runtimeManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RideDao rideDao;

    @Value("${dispatch.process.id}")
    private String processId;

    @Value("${dispatch.assign.driver.expire.duration}")
    private String assignDriverExpireDuration;

    private CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

    @JmsListener(destination = "${listener.destination.ride-event}",
            subscription = "${listener.subscription.ride-event}")
    public void processMessage(String messageAsJson) {

        String messageType = getMessageType(messageAsJson);

        if (messageType.isEmpty() || !accept(messageType)) {
            return;
        }

        switch (messageType) {
            case TYPE_RIDE_REQUESTED_EVENT:
                processRideRequestEvent(messageAsJson);
                break;
            case TYPE_RIDE_STARTED_EVENT:
                processRideStartedEvent(messageAsJson);
                break;
            case TYPE_RIDE_ENDED_EVENT:
                processRideEndedEvent(messageAsJson);
                break;
        }
    }

    private void processRideRequestEvent(String messageAsJson) {
        Message<RideRequestedEvent> message;
        try {

            message = new ObjectMapper().readValue(messageAsJson, new TypeReference<Message<RideRequestedEvent>>() {});

            String rideId = message.getPayload().getRideId();

            log.debug("Processing 'RideRequestedEvent' message for ride " +  rideId);

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
                RuntimeEngine engine = runtimeManager.getRuntimeEngine(ProcessInstanceIdContext.get());
                KieSession ksession = engine.getKieSession();
                rideDao.create(ride);
                try {
                    ProcessInstance pi = ((CorrelationAwareProcessRuntime)ksession).startProcess(processId, correlationKey, parameters);
                    log.debug("Started dispatch process for ride request " + rideId + ". ProcessInstanceId = " + pi.getId());
                    return null;
                } finally {
                    runtimeManager.disposeRuntimeEngine(engine);
                }
            });
        } catch (Exception e) {
            log.error("Error processing msg " + messageAsJson, e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void processRideStartedEvent(String messageAsJson) {
        Message<RideStartedEvent> message;

        try {
            message = new ObjectMapper().readValue(messageAsJson, new TypeReference<Message<RideStartedEvent>>() {});

            String rideId = message.getPayload().getRideId();

            log.debug("Processing 'RideStartedEvent' message for ride " +  rideId);

            CorrelationKey correlationKey = correlationKeyFactory.newCorrelationKey(rideId);

            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.execute((TransactionStatus s) -> {
                RuntimeEngine engine = runtimeManager.getRuntimeEngine(CorrelationKeyContext.get(correlationKey));
                KieSession ksession = engine.getKieSession();
                try {
                    ProcessInstance instance = ((CorrelationAwareProcessRuntime) ksession).getProcessInstance(correlationKey);
                    ksession.signalEvent("RideStarted", null, instance.getId());
                    return null;
                } finally {
                    runtimeManager.disposeRuntimeEngine(engine);
                }
            });
        } catch (Exception e) {
            log.error("Error processing msg " + messageAsJson, e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void processRideEndedEvent(String messageAsJson) {
        Message<RideEndedEvent> message;

        try {
            message = new ObjectMapper().readValue(messageAsJson, new TypeReference<Message<RideEndedEvent>>() {});

            String rideId = message.getPayload().getRideId();

            log.debug("Processing 'RideEndedEvent' message for ride " +  rideId);

            CorrelationKey correlationKey = correlationKeyFactory.newCorrelationKey(rideId);

            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.execute((TransactionStatus s) -> {
                RuntimeEngine engine = runtimeManager.getRuntimeEngine(CorrelationKeyContext.get(correlationKey));
                KieSession ksession = engine.getKieSession();
                try {
                    ProcessInstance instance = ((CorrelationAwareProcessRuntime) ksession).getProcessInstance(correlationKey);
                    ksession.signalEvent("RideEnded", null, instance.getId());
                    return null;
                } finally {
                    runtimeManager.disposeRuntimeEngine(engine);
                }
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
