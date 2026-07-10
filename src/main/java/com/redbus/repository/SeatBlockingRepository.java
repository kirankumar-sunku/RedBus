package com.redbus.repository;

import com.redbus.entity.SeatBlocking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatBlockingRepository extends JpaRepository<SeatBlocking, Long> {

    // All active (confirmed OR non-expired) blocked seats for a bus
    @Query("SELECT s.seatNumber FROM SeatBlocking s WHERE s.busId = :busId " +
           "AND (s.isConfirmed = TRUE OR s.expiresAt > :now)")
    List<Integer> findBlockedSeatNumbers(@Param("busId") Long busId,
                                         @Param("now") LocalDateTime now);

    // Check if a specific seat is currently blocked/confirmed
    @Query("SELECT s FROM SeatBlocking s WHERE s.busId = :busId AND s.seatNumber = :seatNumber " +
           "AND (s.isConfirmed = TRUE OR s.expiresAt > :now)")
    Optional<SeatBlocking> findActiveSeat(@Param("busId") Long busId,
                                          @Param("seatNumber") Integer seatNumber,
                                          @Param("now") LocalDateTime now);

    // For scheduler: expired unconfirmed seats
    List<SeatBlocking> findByIsConfirmedFalseAndExpiresAtBefore(LocalDateTime now);

    // Find confirmed booking by blocking ID (for cancellation)
    Optional<SeatBlocking> findByIdAndIsConfirmedTrue(Long id);

    // All seat blocks (active or expired) belonging to a user — used for cache eviction on signOut
    List<SeatBlocking> findByUserId(Long userId);
}
