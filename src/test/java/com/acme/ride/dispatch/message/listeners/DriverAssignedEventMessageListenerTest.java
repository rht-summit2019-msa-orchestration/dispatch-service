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

import java.util.HashMap;

import com.acme.ride.dispatch.dao.RideDao;
import com.acme.ride.dispatch.entity.Ride;
import io.opentracing.Tracer;
import org.jbpm.process.instance.ProcessInstance;
import org.jbpm.services.api.ProcessService;
import org.junit.Before;
import org.junit.Test;
import org.kie.internal.process.CorrelationKey;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

public class DriverAssignedEventMessageListenerTest {

    private DriverAssignedEventMessageListener messageListener;

    @Mock
    private PlatformTransactionManager ptm;

    @Mock
    private TransactionStatus transactionStatus;

    @Mock
    private ProcessService processService;

    @Mock
    private ProcessInstance processInstance;

    @Mock
    private RideDao rideDao;

    @Mock
    private Tracer tracer;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @Captor
    private ArgumentCaptor<CorrelationKey> correlationKeyCaptor;

    @Before
    public void init() {
        initMocks(this);
        messageListener = new DriverAssignedEventMessageListener();
        setField(messageListener, null, ptm, PlatformTransactionManager.class);
        setField(messageListener, null, processService, ProcessService.class);
        setField(messageListener, null, rideDao, RideDao.class);
        setField(messageListener, null, tracer, Tracer.class);
        when(ptm.getTransaction(any())).thenReturn(transactionStatus);
    }

    @Test
    public void testProcessMessage() {

        String json = "{\"messageType\":\"DriverAssignedEvent\"," +
                "\"id\":\"messageId\"," +
                "\"traceId\":\"trace\"," +
                "\"sender\":\"messageSender\"," +
                "\"timestamp\":1521148332397," +
                "\"payload\":{\"rideId\":\"ride-1234\"," +
                "\"driverId\": \"driver\"}}";

        Ride ride = new Ride();
        ride.setRideId("ride-1234");
        ride.setStatus(Ride.Status.REQUESTED);

        when(rideDao.findByRideId("ride-1234")).thenReturn(ride);

        Long id = 100L;
        when(processService.getProcessInstance(any(CorrelationKey.class))).thenReturn(processInstance);
        when(processInstance.getId()).thenReturn(id);

        messageListener.processMessage(json, "ride-1234", "mytopic", 1, new HashMap<>());

        verify(processService).getProcessInstance(correlationKeyCaptor.capture());
        CorrelationKey correlationKey = correlationKeyCaptor.getValue();
        assertThat(correlationKey.getProperties().get(0).getValue(), equalTo("ride-1234"));
        verify(processService).signalProcessInstance(eq(id), messageCaptor.capture(), isNull() );
        String message = messageCaptor.getValue();
        assertThat(message, equalTo("DriverAssigned"));
        verify(rideDao).findByRideId("ride-1234");
        assertThat(ride.getDriverId(), equalTo("driver"));
    }

    @Test
    public void testProcessMessageWrongMessageType() {

        String json = "{\"messageType\":\"WrongType\"," +
                "\"id\":\"messageId\"," +
                "\"traceId\":\"trace\"," +
                "\"sender\":\"messageSender\"," +
                "\"timestamp\":1521148332397," +
                "\"payload\":{\"rideId\":\"ride-1234\"," +
                "\"driverId\": \"driver\"}}";

        messageListener.processMessage(json, "ride-1234", "mytopic", 1, new HashMap<>());

        verify(processService, never()).signalProcessInstance(any(), any(), any());
        verify(rideDao, never()).findByRideId(any());
    }

    @Test
    public void testProcessMessageWrongMessage() {
        String json = "{\"field1\":\"value1\"," +
                "\"field2\":\"value2\"}";

        messageListener.processMessage(json, "ride-1234", "mytopic", 1, new HashMap<>());

        verify(processService, never()).signalProcessInstance(any(), any(), any());
        verify(rideDao, never()).findByRideId(any());
    }
}
