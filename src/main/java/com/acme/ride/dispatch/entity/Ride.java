package com.acme.ride.dispatch.entity;

import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@SequenceGenerator(name="RideSeq", sequenceName="RIDE_SEQ")
@Table(name = "Ride")
public class Ride {

    public final static int REQUESTED = 1;

    public final static int DRIVER_ASSIGNED = 2;

    public final static int DRIVER_CANCELED = 3;

    public final static int PASSENGER_CANCELED = 4;

    public final static int STARTED = 5;

    public final static int ENDED = 6;

    public final static int EXPIRED = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator="RideSeq")
    private long id;

    private String rideId;

    private String pickup;

    private String destination;

    private int status;

    private BigDecimal price;

    private String passengerId;

    private String driverId;

    public long getId() {
        return id;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getPickup() {
        return pickup;
    }

    public void setPickup(String pickup) {
        this.pickup = pickup;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(String passengerId) {
        this.passengerId = passengerId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public void setStatusAsString(String statusAsString) {
        if ("REQUESTED".equalsIgnoreCase(statusAsString)) {
            status = REQUESTED;
        } else if ("DRIVER_ASSIGNED".equalsIgnoreCase(statusAsString)) {
            status = DRIVER_ASSIGNED;
        } else if ("DRIVER_CANCELED".equalsIgnoreCase(statusAsString)) {
            status = DRIVER_CANCELED;
        } else if ("PASSENGER_CANCELED".equalsIgnoreCase(statusAsString)) {
            status = PASSENGER_CANCELED;
        } else if ("STARTED".equalsIgnoreCase(statusAsString)) {
            status = STARTED;
        } else if ("ENDED".equalsIgnoreCase(statusAsString)) {
            status = ENDED;
        } else if ("EXPIRED".equalsIgnoreCase(statusAsString)) {
            status = EXPIRED;
        }

    }
}
