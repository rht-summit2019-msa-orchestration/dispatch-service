package com.acme.ride.dispatch.entity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@Entity
@SequenceGenerator(name="RideSeq", sequenceName="RIDE_SEQ")
@Table(name = "Ride")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator="RideSeq")
    private long id;

    private String rideId;

    private String pickup;

    private String destination;

    private Integer status = Status.REQUESTED.statusCode;

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

    public Status getStatus() {
        return Status.get(status);
    }

    public void setStatus(Status status) {
        this.status = status.statusCode();
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

    public enum Status {
        REQUESTED(1,"requested"),
        DRIVER_ASSIGNED(2, "driver_assigned"),
        DRIVER_CANCELED(3, "driver_canceled"),
        PASSENGER_CANCELED(4, "passenger_canceled"),
        STARTED(5, "started"),
        ENDED(6, "ended"),
        EXPIRED(7, "expired");

        private static final Map<String,Status> ENUM_MAP_BY_NAME;

        private static final Map<Integer,Status> ENUM_MAP_BY_CODE;

        private final int statusCode;

        private String name;

        static {
            Map<String,Status> mapByName = new HashMap<String,Status>();
            Map<Integer,Status> mapByCode = new HashMap<Integer,Status>();
            for (Status instance : Status.values()) {
                mapByName.put(instance.name,instance);
                mapByCode.put(instance.statusCode, instance);
            }
            ENUM_MAP_BY_CODE = Collections.unmodifiableMap(mapByCode);
            ENUM_MAP_BY_NAME = Collections.unmodifiableMap(mapByName);
        }

        Status(int statusCode, String name) {
            this.statusCode = statusCode;
            this.name = name;
        }

        public int statusCode() {
            return statusCode;
        }

        public String statusName() {
            return name;
        }

        public static Status get(String name) {
            if (name == null) {
                return null;
            }
            return ENUM_MAP_BY_NAME.get(name.toLowerCase());
        }

        public static Status get(int code) {
            return ENUM_MAP_BY_CODE.get(code);
        }
    }
}
