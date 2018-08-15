package com.acme.ride.dispatch.message.model;

public class PassengerCanceledEvent {

    String rideId;

    String reason;

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
