package com.acme.ride.dispatch.message.listeners;

import java.util.HashMap;
import java.util.Map;

import com.acme.ride.dispatch.message.model.Message;
import com.acme.ride.dispatch.message.model.RideRequestedEvent;
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
public class RideRequestedEventMessageListener {

    private final static Logger log = LoggerFactory.getLogger(RideRequestedEventMessageListener.class);

    @Autowired
    private RuntimeManager runtimeManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${dispatch.process.id}")
    private String processId;

    private CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

    @JmsListener(destination = "${listener.destination.ride-requested-event}",
            subscription = "${listener.subscription.ride-requested-event}")
    public void processMessage(String messageAsJson) {

        if (!accept(messageAsJson)) {
            return;
        }
        Message<RideRequestedEvent> message;
        try {

            message = new ObjectMapper().readValue(messageAsJson, new TypeReference<Message<RideRequestedEvent>>() {});

            String rideId = message.getPayload().getRideId();
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("rideId", rideId);
            parameters.put("traceId", message.getTraceId());

            CorrelationKey correlationKey = correlationKeyFactory.newCorrelationKey(rideId);

            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.execute((TransactionStatus s) -> {
                RuntimeEngine engine = runtimeManager.getRuntimeEngine(ProcessInstanceIdContext.get());
                KieSession ksession = engine.getKieSession();
                try {
                    ProcessInstance pi = ((CorrelationAwareProcessRuntime)ksession).startProcess(processId, correlationKey, parameters);
                    log.info("Started dispatch process for ride request " + rideId + ". ProcessInstanceId = " + pi.getId());
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
            if (!"RideRequestedEvent".equalsIgnoreCase(messageType) ) {
                log.info("Message with type '" + messageType + "' is ignored");
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Unexpected message without 'messageType' field.");
            return false;
        }
    }

}
