package com.acme.ride.dispatch.wih;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.acme.ride.dispatch.dao.RideDao;
import com.acme.ride.dispatch.entity.Ride;
import com.acme.ride.dispatch.message.model.AssignDriverCommand;
import com.acme.ride.dispatch.message.model.HandlePaymentCommand;
import com.acme.ride.dispatch.message.model.Message;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

@Component("SendMessage")
public class MessageSenderWorkItemHandler implements WorkItemHandler {

    private static final Logger log = LoggerFactory.getLogger(MessageSenderWorkItemHandler.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private KafkaTemplate<String, Message<?>> kafkaTemplate;

    @Autowired
    private RideDao rideDao;

    private Map<String, Function<Ride, ?>> payloadBuilders = new HashMap<>();

    public MessageSenderWorkItemHandler() {
        addPayloadBuilder("AssignDriverCommand", AssignDriverCommand::build);
        addPayloadBuilder("HandlePaymentCommand", HandlePaymentCommand::build);
    }

    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        Map<String, Object> parameters = workItem.getParameters();
        Object messageType = parameters.get("MessageType");
        Object destinationParam = parameters.get("Destination");
        if (!(messageType instanceof String) || !(destinationParam instanceof String)) {
            throw new IllegalStateException("Parameters 'messageType', 'destination' cannot be null and must be of type String");
        }
        String destination = applicationContext.getEnvironment().getProperty((String)destinationParam);
        if (destination == null || destination.isEmpty()) {
            throw new IllegalStateException("Destination cannot be null or empty. '" + destinationParam + "' environment property not set" );
        }
        Function<Ride, ?> builder = payloadBuilders.get(messageType);
        if (builder == null) {
            throw new IllegalStateException("No builder found for payload'" + messageType + "'");
        }
        Object traceId = parameters.get("traceId");
        if (!(traceId instanceof String)) {
            log.warn("Parameter traceId not found or not a String. Ignoring");
            traceId = "";
        }
        Object rideId = parameters.get("rideId");
        if (!(rideId instanceof String)) {
            throw new IllegalStateException("\"Parameters 'rideId' cannot be null and must be of type String\"");
        }
        Ride ride = rideDao.findByRideId((String)rideId);
        Message<Object> message  = new Message.Builder<Object>((String)messageType, "DispatchService", builder.apply(ride))
                .traceId(traceId.toString()).build();
        send((String) rideId, message, destination);
        manager.completeWorkItem(workItem.getId(), Collections.emptyMap());
    }

    private void send(String rideId, Message<?> msg, String destination) {
        ListenableFuture<SendResult<String, Message<?>>> future = kafkaTemplate.send(destination, rideId, msg);
        future.addCallback(
                result -> log.debug("Sent '" + msg.getMessageType() + "' message for ride " + rideId),
                ex -> log.error("Error sending '" + msg.getMessageType() + "' message for ride " + rideId, ex));
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {

    }

    void addPayloadBuilder(String payloadType, Function<Ride, ? extends Object> builder) {
        payloadBuilders.put(payloadType, builder);
    }
}
