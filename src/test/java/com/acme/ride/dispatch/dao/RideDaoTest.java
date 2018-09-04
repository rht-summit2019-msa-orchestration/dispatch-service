package com.acme.ride.dispatch.dao;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import com.acme.ride.dispatch.DataSourceConfiguration;
import com.acme.ride.dispatch.JpaConfiguration;
import com.acme.ride.dispatch.entity.Ride;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.autoconfigure.transaction.jta.NarayanaJtaConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {RideDao.class, JpaConfiguration.class, JpaProperties.class, DataSourceConfiguration.class,
        NarayanaJtaConfiguration.class, DataSourceProperties.class})
public class RideDaoTest {

    @Autowired
    private RideDao rideDao;

    @Test
    @Transactional
    public void testSaveOrder() {
        assertThat(rideDao, notNullValue());

        Ride ride = new Ride();
        ride.setRideId(UUID.randomUUID().toString());
        ride.setPassengerId("passengerId");
        ride.setDriverId("driverId");
        ride.setDestination("destination");
        ride.setPickup("pickup");
        ride.setPrice(new BigDecimal("50.00"));
        ride.setStatus(Ride.Status.REQUESTED);

        rideDao.create(ride);
        assertThat(ride.getId(), not(equalTo(0)));
    }

    @Test
    @Transactional
    public void testFindOrder() {
        assertThat(rideDao, notNullValue());

        Ride ride = new Ride();
        ride.setRideId(UUID.randomUUID().toString());
        ride.setPassengerId("passengerId");
        ride.setDriverId("driverId");
        ride.setDestination("destination");
        ride.setPickup("pickup");
        ride.setPrice(new BigDecimal("50.00"));
        ride.setStatus(Ride.Status.REQUESTED);

        rideDao.create(ride);

        Ride found = rideDao.find(ride.getId());
        assertThat(found, notNullValue());
        assertThat(found.getId(), equalTo(ride.getId()));
        assertThat(found.getRideId(), equalTo(ride.getRideId()));

    }

    @Test
    @Transactional
    public void testFindOrderByRefId() {
        assertThat(rideDao, notNullValue());

        Ride ride = new Ride();
        ride.setRideId(UUID.randomUUID().toString());
        ride.setPassengerId("passengerId");
        ride.setDriverId("driverId");
        ride.setDestination("destination");
        ride.setPickup("pickup");
        ride.setPrice(new BigDecimal("50.00"));
        ride.setStatus(Ride.Status.REQUESTED);

        rideDao.create(ride);

        Ride found = rideDao.findByRideId(ride.getRideId());
        assertThat(found, notNullValue());
        assertThat(found.getId(), equalTo(ride.getId()));
        assertThat(found.getRideId(), equalTo(ride.getRideId()));
    }

    @Test
    @Transactional
    public void testFindOrderByRefIdNotFound() {
        assertThat(rideDao, notNullValue());

        Ride found = rideDao.findByRideId(UUID.randomUUID().toString());
        assertThat(found, nullValue());
    }


}
