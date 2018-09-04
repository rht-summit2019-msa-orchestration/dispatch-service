package com.acme.ride.dispatch.message.listeners;

import com.acme.ride.dispatch.dao.RideDao;
import com.acme.ride.dispatch.entity.Ride;
import com.acme.ride.dispatch.message.model.DriverAssignedEvent;
import com.acme.ride.dispatch.message.model.Message;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class DriverAssignedEventMessageListener {

    private final static Logger log = LoggerFactory.getLogger(DriverAssignedEventMessageListener.class);

    @Autowired
    private RuntimeManager runtimeManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RideDao rideDao;

    private CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

    @JmsListener(destination = "${listener.destination.driver-assigned-event}",
            subscription = "${listener.subscription.driver-assigned-event}")
    public void processMessage(String messageAsJson) {

        if (!accept(messageAsJson)) {
            return;
        }
        Message<DriverAssignedEvent> message;
        try {

            message = new ObjectMapper().readValue(messageAsJson, new TypeReference<Message<DriverAssignedEvent>>() {});

            String rideId = message.getPayload().getRideId();

            log.debug("Processing 'DriverAssignedEvent' message for ride " +  rideId);

            CorrelationKey correlationKey = correlationKeyFactory.newCorrelationKey(rideId);

            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.execute((TransactionStatus s) -> {
                RuntimeEngine engine = runtimeManager.getRuntimeEngine(CorrelationKeyContext.get(correlationKey));
                KieSession ksession = engine.getKieSession();
                try {
                    Ride ride = rideDao.findByRideId(rideId);
                    if (! Ride.Status.REQUESTED.equals(ride.getStatus())) {
                        // handle inconsistent state
                        log.warn("Ride " + rideId + ". Status: " + ride.getStatus() + ". Expected: " + Ride.Status.REQUESTED);
                        return null;
                    }
                    ride.setDriverId(message.getPayload().getDriverId());
                    ride.setStatus(Ride.Status.DRIVER_ASSIGNED);
                    ProcessInstance instance = ((CorrelationAwareProcessRuntime) ksession).getProcessInstance(correlationKey);
                    ksession.signalEvent("DriverAssigned", null, instance.getId());
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

    private boolean accept(String messageAsJson) {
        try {
            String messageType = JsonPath.read(messageAsJson, "$.messageType");
            if (!"DriverAssignedEvent".equalsIgnoreCase(messageType) ) {
                log.debug("Message with type '" + messageType + "' is ignored");
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Unexpected message without 'messageType' field.");
            return false;
        }
    }

}
