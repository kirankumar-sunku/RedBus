package com.redbus.repository;

import com.redbus.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    // History: all confirmed bookings for a user (join via seat_blocking)
    @Query("SELECT p FROM Passenger p JOIN SeatBlocking s ON p.blockingId = s.id WHERE s.userId = :userId")
    List<Passenger> findByUserId(@Param("userId") Long userId);

    Optional<Passenger> findByBlockingId(Long blockingId);

    @Query("SELECT p.seatNumber FROM Passenger p WHERE p.busId = :busId")
    List<Integer> findBookedSeatNumbersByBusId(@Param("busId") Long busId);
}
