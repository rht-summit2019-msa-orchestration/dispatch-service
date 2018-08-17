package com.acme.ride.dispatch.message.model;

import java.math.BigDecimal;

import com.acme.ride.dispatch.entity.Ride;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class HandlePaymentCommand {

    private String rideId;

    private String passengerId;

    private BigDecimal price;


    public static HandlePaymentCommand build(Ride ride) {

        HandlePaymentCommand command = new HandlePaymentCommand();
        command.rideId = ride.getRideId();
        command.passengerId = ride.getPassengerId();
        command.price = ride.getPrice();

        return command;
    }
}
