package com.acme.ride.dispatch.wih;

import java.util.Collections;

import com.acme.ride.dispatch.dao.RideDao;
import com.acme.ride.dispatch.entity.Ride;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateRideWorkItemhandler implements WorkItemHandler {

    @Autowired
    private RideDao rideDao;

    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        Object rideId = workItem.getParameters().get("rideId");
        if (!(rideId instanceof String)) {
            throw new IllegalStateException("Parameter 'rideId' cannot be null and must be of type String");
        }
        Object status = workItem.getParameter("status");
        if (!(status instanceof String)) {
            throw new IllegalStateException("Parameter 'status' cannot be null and must be of type String");
        }
        Ride ride = rideDao.findByRideId((String) rideId);
        if (ride == null) {
            throw new IllegalStateException("Ride with rideId " + rideId + " not found");
        }
        ride.setStatusAsString((String) status);
        manager.completeWorkItem(workItem.getId(), Collections.emptyMap());
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {

    }
}
