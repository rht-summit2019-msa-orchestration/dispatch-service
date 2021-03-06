package com.acme.ride.dispatch.wih;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.util.HashMap;
import java.util.Map;

import com.acme.ride.dispatch.dao.RideDao;
import com.acme.ride.dispatch.entity.Ride;
import com.acme.ride.dispatch.message.model.Message;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.concurrent.SettableListenableFuture;

public class MessageSenderWorkItemHandlerTest {

    @Mock
    private KafkaTemplate kafkaTemplate;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private Environment environment;

    @Mock
    private RideDao rideDao;

    @Mock
    private WorkItem workItem;

    @Mock
    private WorkItemManager workItemManager;

    private MessageSenderWorkItemHandler wih;

    @Before
    public void setup() {
        initMocks(this);
        wih = new MessageSenderWorkItemHandler();
        setField(wih, null, kafkaTemplate, KafkaTemplate.class);
        setField(wih, null, applicationContext, ApplicationContext.class);
        setField(wih, null, rideDao, RideDao.class);
        when(applicationContext.getEnvironment()).thenReturn(environment);
    }

    @Test
    public void testExecuteWorkItem() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("MessageType", "testMessageType");
        parameters.put("Destination", "test.destination");
        parameters.put("traceId", "testTraceID");
        parameters.put("rideId", "testRideId");
        when(workItem.getParameters()).thenReturn(parameters);
        when(workItem.getId()).thenReturn(1L);

        when(environment.getProperty("test.destination")).thenReturn("topic.destination.test");

        Ride ride = new Ride();
        when(rideDao.findByRideId("testRideId")).thenReturn(ride);

        when(kafkaTemplate.send(any(String.class), any(String.class), any(Message.class))).thenReturn(new SettableListenableFuture());

        wih.addPayloadBuilder("testMessageType", TestMessageEvent::build);

        wih.executeWorkItem(workItem, workItemManager);
        verify(workItemManager).completeWorkItem(eq(1L), anyMap());
        verify(kafkaTemplate).send(eq("topic.destination.test"), eq("testRideId"), any(Message.class));
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class TestMessageEvent {

        private Ride ride;

        public static TestMessageEvent build(Ride ride) {
            TestMessageEvent event = new TestMessageEvent();
            event.ride =ride;
            return event;
        }

    }
}
