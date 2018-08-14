package com.acme.ride.dispatch.message.model;

import java.math.BigDecimal;

import com.acme.ride.dispatch.entity.Ride;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AssignDriverCommand {

    private String rideId;

    private String pickup;

    private String destination;

    private BigDecimal price;

    private String passengerId;

    public static AssignDriverCommand build(Ride ride) {

        AssignDriverCommand command = new AssignDriverCommand();
        command.rideId = ride.getRideId();
        command.pickup = ride.getPickup();
        command.destination = ride.getDestination();
        command.passengerId = ride.getPassengerId();
        command.price = ride.getPrice();

        return command;
    }
}
