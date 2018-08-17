package com.acme.ride.dispatch.message.model;

import java.util.Date;

public class RideEndedEvent {

    private String rideId;

    private Date timestamp;

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
