package com.redbus.repository;

import com.redbus.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BusRepository extends JpaRepository<Bus, Long> {

    List<Bus> findByFromLocationIgnoreCaseAndToLocationIgnoreCaseAndDateOfJourney(String from, String to, LocalDate date);

    @Modifying
    @Query("UPDATE Bus b SET b.availableSeatsCount = b.availableSeatsCount - 1 WHERE b.busId = :busId AND b.availableSeatsCount > 0")
    int decrementAvailableSeats(@Param("busId") Long busId);

    @Modifying
    @Query("UPDATE Bus b SET b.availableSeatsCount = b.availableSeatsCount + 1 WHERE b.busId = :busId AND b.availableSeatsCount < b.totalSeats")
    int incrementAvailableSeats(@Param("busId") Long busId);

    List<Bus> findAll();
}
