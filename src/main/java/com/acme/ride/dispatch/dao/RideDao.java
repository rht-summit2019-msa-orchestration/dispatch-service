package com.acme.ride.dispatch.dao;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;

import com.acme.ride.dispatch.entity.Ride;
import org.springframework.stereotype.Component;

@Component
public class RideDao {

    @PersistenceContext
    private EntityManager entityManager;

    public void create(Ride ride) {
        entityManager.persist(ride);
    }

    public List<Ride> findAll(){
        return entityManager.createQuery( "from " + Ride.class.getName() )
                .getResultList();
    }

    public Ride find(long id) {
        Ride order =  entityManager.find(Ride.class, id);
        if (order == null) {
            throw new EntityNotFoundException("Ride with id " + id + " not found.");
        }
        return order;
    }

    public Ride findByRideId(String rideId) {
        List<Ride> rideList = entityManager.createQuery("SELECT o FROM Ride o WHERE o.rideId = :rideId")
                .setParameter("rideId", rideId)
                .getResultList();
        if (rideList.isEmpty()) {
            return null;
        }
        return rideList.get(0);
    }

    public void delete(Ride ride){
        entityManager.remove(ride);
    }



}
