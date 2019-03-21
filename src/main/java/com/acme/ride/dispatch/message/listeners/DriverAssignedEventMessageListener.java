package com.acme.ride.dispatch.message.listeners;

import java.util.Map;
import java.util.Optional;

import com.acme.ride.dispatch.dao.RideDao;
import com.acme.ride.dispatch.entity.Ride;
import com.acme.ride.dispatch.message.model.DriverAssignedEvent;
import com.acme.ride.dispatch.message.model.Message;
import com.acme.ride.dispatch.tracing.TracingKafkaUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
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
public class DriverAssignedEventMessageListener {

    private final static Logger log = LoggerFactory.getLogger(DriverAssignedEventMessageListener.class);

    @Autowired
    private ProcessService processService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RideDao rideDao;

    @Autowired
    private Tracer tracer;

    private CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

    @KafkaListener(topics = "${listener.destination.driver-assigned-event}")
    public void processMessage(@Payload String messageAsJson, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                               @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                               @Headers Map<String, Object> headers) {

        //create new span
        Scope scope = TracingKafkaUtils.buildChildSpan("processRideEventMessage", headers, tracer);

        try {

            if (!accept(messageAsJson)) {
                return;
            }

            Message<DriverAssignedEvent> message = new ObjectMapper().readValue(messageAsJson,
                    new TypeReference<Message<DriverAssignedEvent>>() {});

            String rideId = message.getPayload().getRideId();

            log.debug("Processing 'DriverAssignedEvent' message for ride " + key + " from topic:partition " + topic + ":" + partition);

            CorrelationKey correlationKey = correlationKeyFactory.newCorrelationKey(rideId);

            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.execute((TransactionStatus s) -> {
                Ride ride = rideDao.findByRideId(rideId);
                ride.setDriverId(message.getPayload().getDriverId());
                ProcessInstance instance = processService.getProcessInstance(correlationKey);
                processService.signalProcessInstance(instance.getId(), "DriverAssigned", null);
                return null;
            });
        } catch (Exception e) {
            log.error("Error processing msg " + messageAsJson, e);
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            if (scope != null) {
                scope.close();
            }
        }
    }

    private boolean accept(String messageAsJson) {
        try {
            String messageType = JsonPath.read(messageAsJson, "$.messageType");
            if ("DriverAssignedEvent".equalsIgnoreCase(messageType) ) {
                return true;
            } else {
                log.debug("Message with type '" + messageType + "' is ignored");
            }
        } catch (Exception e) {
            log.warn("Unexpected message without 'messageType' field.");
        }
        Optional.ofNullable(tracer.activeSpan()).ifPresent(s -> new StringTag("msg.accepted").set(s, "false"));
        return false;
    }

}
