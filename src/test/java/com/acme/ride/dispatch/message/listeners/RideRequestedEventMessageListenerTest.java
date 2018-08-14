package com.acme.ride.dispatch.message.listeners;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.util.Map;

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

public class RideRequestedEventMessageListenerTest {

    private RideRequestedEventMessageListener messageListener;

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

    @Captor
    private ArgumentCaptor<String> processIdCaptor;

    @Captor
    private ArgumentCaptor<CorrelationKey> correlationKeyCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> parametersCaptor;

    private String processId = "dispatch";

    @Before
    public void init() {
        initMocks(this);
        messageListener = new RideRequestedEventMessageListener();
        setField(messageListener, null, ptm, PlatformTransactionManager.class);
        setField(messageListener, null, runtimeManager, RuntimeManager.class);
        setField(messageListener, "processId", processId, String.class);
        when(ptm.getTransaction(any())).thenReturn(transactionStatus);
        when(runtimeManager.getRuntimeEngine(any())).thenReturn(runtimeEngine);
        when(runtimeEngine.getKieSession()).thenReturn(kieSession);
        when(kieSession.startProcess(any(), any(), any())).thenReturn(processInstance);
    }

    @Test
    public void testProcessMessage() {

        String json = "{\"messageType\":\"RideRequestedEvent\"," +
                "\"id\":\"messageId\"," +
                "\"traceId\":\"trace\"," +
                "\"sender\":\"messageSender\"," +
                "\"timestamp\":1521148332397," +
                "\"payload\":{\"rideId\":\"ride123\"," +
                "\"pickup\": \"pickup\", \"destination\": \"destination\"," +
                "\"price\": 25.0, \"passengerId\": \"passenger\"}}";

        messageListener.processMessage(json);

        verify(kieSession).startProcess(processIdCaptor.capture(), correlationKeyCaptor.capture(), parametersCaptor.capture());
        assertThat(processIdCaptor.getValue(), equalTo(processId));
        CorrelationKey correlationKey = correlationKeyCaptor.getValue();
        assertThat(correlationKey.getName(), equalTo("ride123"));
        Map<String, Object> parameters = parametersCaptor.getValue();
        assertThat(parameters.size(), equalTo(2));
        assertThat(parameters.get("traceId"), equalTo("trace"));
        assertThat(parameters.get("rideId"), equalTo("ride123"));
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

    }

    @Test
    public void testProcessMessageWrongMessage() {
        String json = "{\"field1\":\"value1\"," +
                "\"field2\":\"value2\"}";

        messageListener.processMessage(json);

        verify(kieSession, never()).startProcess(any(), any(), any());

    }
}
