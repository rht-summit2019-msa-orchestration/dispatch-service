package com.acme.ride.dispatch.message.listeners;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import com.acme.ride.dispatch.dao.RideDao;
import com.acme.ride.dispatch.entity.Ride;
import com.acme.ride.dispatch.message.model.RideStartedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.drools.core.command.impl.CommandBasedStatefulKnowledgeSession;
import org.jbpm.process.instance.ProcessInstance;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.internal.process.CorrelationKey;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

public class RideEventsMessageListenerTest {

    private RideEventsMessageListener messageListener;

    @Mock
    private PlatformTransactionManager ptm;

    @Mock
    private TransactionStatus transactionStatus;

    @Mock
    private RuntimeManager runtimeManager;

    @Mock
    private RuntimeEngine runtimeEngine;

    @Mock
    private CommandBasedStatefulKnowledgeSession kieSession;

    @Mock
    private ProcessInstance processInstance;

    @Mock
    private RideDao rideDao;

    @Captor
    private ArgumentCaptor<Ride> rideCaptor;

    @Captor
    private ArgumentCaptor<String> processIdCaptor;

    @Captor
    private ArgumentCaptor<CorrelationKey> correlationKeyCaptor;

    @Captor
    private ArgumentCaptor<String> signalCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> parametersCaptor;

    private String processId = "dispatch";

    @Before
    public void init() {
        initMocks(this);
        messageListener = new RideEventsMessageListener();
        setField(messageListener, null, ptm, PlatformTransactionManager.class);
        setField(messageListener, null, runtimeManager, RuntimeManager.class);
        setField(messageListener, "processId", processId, String.class);
        setField(messageListener, null, rideDao, RideDao.class);
        setField(messageListener, "assignDriverExpireDuration", "5M", String.class);
        when(ptm.getTransaction(any())).thenReturn(transactionStatus);
        when(runtimeManager.getRuntimeEngine(any())).thenReturn(runtimeEngine);
        when(runtimeEngine.getKieSession()).thenReturn(kieSession);
        when(kieSession.startProcess(any(), any(), any())).thenReturn(processInstance);
    }

    @Test
    public void testProcessRideRequestedEventMessage() {

        String json = "{\"messageType\":\"RideRequestedEvent\"," +
                "\"id\":\"messageId\"," +
                "\"traceId\":\"trace\"," +
                "\"sender\":\"messageSender\"," +
                "\"timestamp\":1521148332397," +
                "\"payload\":{\"rideId\":\"ride123\"," +
                "\"pickup\": \"pickup\", \"destination\": \"destination\"," +
                "\"price\": 25.0, \"passengerId\": \"passenger\"}}";

        messageListener.processMessage(json);

        verify(rideDao).create(rideCaptor.capture());
        Ride ride = rideCaptor.getValue();
        assertThat(ride, notNullValue());
        assertThat(ride.getRideId(), equalTo("ride123"));
        assertThat(ride.getPickup(), equalTo("pickup"));
        assertThat(ride.getDestination(), equalTo("destination"));
        assertThat(ride.getPassengerId(), equalTo("passenger"));
        assertThat(ride.getPrice(), equalTo(new BigDecimal("25.0")));
        assertThat(ride.getStatus(), equalTo(Ride.Status.REQUESTED));

        verify(kieSession).startProcess(processIdCaptor.capture(), correlationKeyCaptor.capture(), parametersCaptor.capture());
        assertThat(processIdCaptor.getValue(), equalTo(processId));
        CorrelationKey correlationKey = correlationKeyCaptor.getValue();
        assertThat(correlationKey.getName(), equalTo("ride123"));
        Map<String, Object> parameters = parametersCaptor.getValue();
        assertThat(parameters.size(), equalTo(3));
        assertThat(parameters.get("traceId"), equalTo("trace"));
        assertThat(parameters.get("rideId"), equalTo("ride123"));
        assertThat(parameters.get("assign_driver_expire_duration"), equalTo("5M"));
    }

    @Test
    public void test() throws Exception {

        RideStartedEvent event = new RideStartedEvent();
        event.setRideId("ref-1234");
        event.setTimestamp(new Date());

        System.out.println(new ObjectMapper().writeValueAsString(event));
    }

    @Test
    public void testProcessRideStartedMessage() {

        String json = "{\"messageType\":\"RideStartedEvent\"," +
                "\"id\":\"messageId\"," +
                "\"traceId\":\"trace\"," +
                "\"sender\":\"messageSender\"," +
                "\"timestamp\":1534336579807," +
                "\"payload\":{\"rideId\":\"ride-1234\"," +
                "\"timestamp\": 1534336579807}}";

        Ride ride = new Ride();
        ride.setRideId("ride-1234");
        ride.setStatus(Ride.Status.DRIVER_ASSIGNED);

        when(rideDao.findByRideId("ride-1234")).thenReturn(ride);

        Long id = 100L;
        when(kieSession.getProcessInstance(any(CorrelationKey.class))).thenReturn(processInstance);
        when(processInstance.getId()).thenReturn(id);

        messageListener.processMessage(json);

        verify(kieSession).getProcessInstance(correlationKeyCaptor.capture());
        CorrelationKey correlationKey = correlationKeyCaptor.getValue();
        assertThat(correlationKey.getProperties().get(0).getValue(), equalTo("ride-1234"));
        verify(kieSession).signalEvent(signalCaptor.capture(), isNull(), eq(id));
        String signal = signalCaptor.getValue();
        assertThat(signal, equalTo("RideStarted"));
        verify(runtimeManager).disposeRuntimeEngine(runtimeEngine);
    }

    @Test
    public void testProcessRideSEndedMessage() {

        String json = "{\"messageType\":\"RideEndedEvent\"," +
                "\"id\":\"messageId\"," +
                "\"traceId\":\"trace\"," +
                "\"sender\":\"messageSender\"," +
                "\"timestamp\":1534336579807," +
                "\"payload\":{\"rideId\":\"ride123\"," +
                "\"timestamp\": 1534336579807}}";

        Ride ride = new Ride();
        ride.setRideId("ride123");
        ride.setStatus(Ride.Status.STARTED);

        when(rideDao.findByRideId("ride123")).thenReturn(ride);

        Long id = 100L;
        when(kieSession.getProcessInstance(any(CorrelationKey.class))).thenReturn(processInstance);
        when(processInstance.getId()).thenReturn(id);

        messageListener.processMessage(json);

        verify(kieSession).getProcessInstance(correlationKeyCaptor.capture());
        CorrelationKey correlationKey = correlationKeyCaptor.getValue();
        assertThat(correlationKey.getProperties().get(0).getValue(), equalTo("ride123"));
        verify(kieSession).signalEvent(signalCaptor.capture(), isNull(), eq(id));
        String signal = signalCaptor.getValue();
        assertThat(signal, equalTo("RideEnded"));
        verify(runtimeManager).disposeRuntimeEngine(runtimeEngine);
    }

    @Test
    public void testProcessMessageWrongMessageType() {

        String json = "{\"messageType\":\"WrongType\"," +
                "\"id\":\"messageId\"," +
                "\"traceId\":\"trace\"," +
                "\"sender\":\"messageSender\"," +
                "\"timestamp\":1521148332397," +
                "\"payload\":{\"rideId\":\"ride123\"," +
                "\"pickup\": \"pickup\", \"destination\": \"destination\"," +
                "\"price\": 25.0, \"passengerId\": \"passenger\"}}";

        messageListener.processMessage(json);

        verify(kieSession, never()).startProcess(any(), any(), any());

        verify(rideDao, never()).create(any());
    }

    @Test
    public void testProcessMessageWrongMessage() {
        String json = "{\"field1\":\"value1\"," +
                "\"field2\":\"value2\"}";

        messageListener.processMessage(json);

        verify(kieSession, never()).startProcess(any(), any(), any());
        verify(rideDao, never()).create(any());
    }
}
