package com.acme.ride.dispatch.message.listeners;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.acme.ride.dispatch.dao.RideDao;
import com.acme.ride.dispatch.entity.Ride;
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

public class PassengerCanceledEventMessageListenerTest {

    private PassengerCanceledEventMessageListener messageListener;

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
    private ArgumentCaptor<String> signalCaptor;

    @Captor
    private ArgumentCaptor<CorrelationKey> correlationKeyCaptor;

    @Before
    public void init() {
        initMocks(this);
        messageListener = new PassengerCanceledEventMessageListener();
        setField(messageListener, null, ptm, PlatformTransactionManager.class);
        setField(messageListener, null, runtimeManager, RuntimeManager.class);
        setField(messageListener, null, rideDao, RideDao.class);
        when(ptm.getTransaction(any())).thenReturn(transactionStatus);
        when(runtimeManager.getRuntimeEngine(any())).thenReturn(runtimeEngine);
        when(runtimeEngine.getKieSession()).thenReturn(kieSession);
    }

    @Test
    public void testProcessMessage() {

        String json = "{\"messageType\":\"PassengerCanceledEvent\"," +
                "\"id\":\"messageId\"," +
                "\"traceId\":\"trace\"," +
                "\"sender\":\"messageSender\"," +
                "\"timestamp\":1521148332397," +
                "\"payload\":{\"rideId\":\"ride-1234\"," +
                "\"reason\": \"driver did not show up\"}}";

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
        assertThat(signal, equalTo("PassengerCanceled"));
        verify(runtimeManager).disposeRuntimeEngine(runtimeEngine);
    }

    @Test
    public void testProcessMessageWrongMessageType() {

        String json = "{\"messageType\":\"WrongType\"," +
                "\"id\":\"messageId\"," +
                "\"traceId\":\"trace\"," +
                "\"sender\":\"messageSender\"," +
                "\"timestamp\":1521148332397," +
                "\"payload\":{\"rideId\":\"ride-1234\"," +
                "\"reason\": \"driver did not show up\"}}";

        messageListener.processMessage(json);

        verify(runtimeManager, never()).getRuntimeEngine(any());
        verify(rideDao, never()).findByRideId(any());
    }

    @Test
    public void testProcessMessageWrongMessage() {
        String json = "{\"field1\":\"value1\"," +
                "\"field2\":\"value2\"}";

        messageListener.processMessage(json);

        verify(runtimeManager, never()).getRuntimeEngine(any());
        verify(rideDao, never()).findByRideId(any());
    }
}
