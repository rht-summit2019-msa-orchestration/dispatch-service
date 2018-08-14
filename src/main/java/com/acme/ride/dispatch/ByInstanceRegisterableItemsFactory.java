package com.acme.ride.dispatch;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jbpm.runtime.manager.impl.DefaultRegisterableItemsFactory;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.WorkItemHandler;

public class ByInstanceRegisterableItemsFactory extends DefaultRegisterableItemsFactory {

    private Map<String, WorkItemHandler> workItemHandlersByInstance = new ConcurrentHashMap<>();

    @Override
    public Map<String, WorkItemHandler> getWorkItemHandlers(RuntimeEngine runtime) {
        Map<String, WorkItemHandler> workItemHandlers = new HashMap<String, WorkItemHandler>(workItemHandlersByInstance);
        workItemHandlers.putAll(super.getWorkItemHandlers(runtime));
        return workItemHandlers;
    }

    public void addWorkItemHandler(String name, WorkItemHandler wih) {
        workItemHandlersByInstance.put(name, wih);
    }
}
