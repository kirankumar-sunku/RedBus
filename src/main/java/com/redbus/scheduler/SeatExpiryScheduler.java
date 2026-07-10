package com.redbus.scheduler;

import com.redbus.entity.SeatBlocking;
import com.redbus.exception.RedBusException;
import com.redbus.repository.BusRepository;
import com.redbus.repository.SeatBlockingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SEAT EXPIRY SCHEDULER
 * Runs every 60 seconds to release seats whose 10-minute blocking window has expired
 * without being confirmed (passenger details not submitted).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SeatExpiryScheduler {

    private final SeatBlockingRepository seatBlockingRepository;
    private final BusRepository busRepository;

    @Scheduled(fixedRate = 60000) // Every 60 seconds
    @Transactional
    public void releaseExpiredSeats() throws RedBusException {
        try {
            List<SeatBlocking> expired = seatBlockingRepository
                    .findByIsConfirmedFalseAndExpiresAtBefore(LocalDateTime.now());

            if (!expired.isEmpty()) {
                log.info("SeatExpiryScheduler: releasing {} expired seat blocks", expired.size());
                for (SeatBlocking sb : expired) {
                    busRepository.incrementAvailableSeats(sb.getBusId());
                    seatBlockingRepository.delete(sb);
                    log.debug("Released expired seat {} for bus {}", sb.getSeatNumber(), sb.getBusId());
                }
            }
        } catch (Exception e) {
            throw new RedBusException("SeatExpiryScheduler: ", "releaseExpiredSeats exception occurred", e);
        }
    }
}
