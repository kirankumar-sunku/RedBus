package com.redbus.service.serviceImpl;

import com.redbus.dto.request.BusRequest;
import com.redbus.dto.response.BusSearchResponse;
import com.redbus.entity.Bus;
import com.redbus.exception.RedBusException;
import com.redbus.exception.ResourceNotFoundException;
import com.redbus.repository.BusRepository;
import com.redbus.repository.PassengerRepository;
import com.redbus.service.serviceInterface.BusServiceInter;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class BusService implements BusServiceInter {

    private final BusRepository busRepository;
    private final PassengerRepository passengerRepository;

    // June-07
// filename: BusService.java
   // @CacheEvict(value = "buses", allEntries = true)
  //  @CachePut(value = "buses", key = "#buses.id()")         // June 16
    @Caching(evict = {
            @CacheEvict(value = "buses", allEntries = true),
            @CacheEvict(value = "allBuses", allEntries = true)
    })
    public Map<String, String> addBus(BusRequest request) throws RedBusException {
        try {
            System.out.println("Request received: " + request);
            System.out.println("addBus = " + LocalTime.now());
            // Validate request
            if (request == null) {
                throw new IllegalArgumentException("Bus request cannot be null");
            }

            // Parse date with error handling
            LocalDate journeyDate;
            try {
                journeyDate = LocalDate.parse(request.getDateOfJourney());
                System.out.println("Parsed date: " + journeyDate);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format. Expected yyyy-MM-dd, got: " + request.getDateOfJourney());
            }

            // Build bus object
            Bus bus = Bus.builder()
                    .travelsName(request.getTravelsName())
                    .fromLocation(request.getFromLocation())
                    .toLocation(request.getToLocation())
                    .dateOfJourney(journeyDate)
                    .price(request.getPrice())
                    .totalSeats(request.getTotalSeats())
                    .availableSeatsCount(request.getAvailableSeatsCount())
                    .build();

            // Save to database
            Bus savedBus = busRepository.save(bus);
            System.out.println("Bus saved successfully with ID: " + savedBus.getBusId());

            return Map.of(
                    "message", "Bus added successfully",
                    "busId", String.valueOf(savedBus.getBusId())
            );

        } catch (Exception e) {
            throw new RedBusException("BusService: ", "addBus exception occurred", e);
        }
    }

    /*@Caching(evict = {
            @CacheEvict(value = "buses", allEntries = true),
            @CacheEvict(value = "availableSeats", key = "#busId")
    })*/
    // @CachePut(value = "buses", key = "#busId")         // June 16
    @Caching(evict = {
            @CacheEvict(value = "buses", allEntries = true),
            @CacheEvict(value = "allBuses", allEntries = true),
            @CacheEvict(value = "availableSeats", key = "#busId")
    })
    public Map<String, String> updateBus(Long busId, BusRequest request) throws RedBusException {
        try {
            System.out.println("updateBus service1 = " + LocalTime.now());
            Bus bus = busRepository.findById(busId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));
            System.out.println("bus = " + bus);
            if (request.getTravelsName() != null) bus.setTravelsName(request.getTravelsName());
            System.out.println("getTravelsName = " + request.getTravelsName());
            if (request.getFromLocation() != null) bus.setFromLocation(request.getFromLocation());
            if (request.getToLocation() != null) bus.setToLocation(request.getToLocation());
            if (request.getDateOfJourney() != null) bus.setDateOfJourney(LocalDate.parse(request.getDateOfJourney()));
            System.out.println("Date of Journey = " + request.getDateOfJourney());
            if (request.getPrice() != null) bus.setPrice(request.getPrice());
            System.out.println("request.getAvailableSeatsCount() = " + request.getAvailableSeatsCount());
            if (request.getAvailableSeatsCount() != null) bus.setAvailableSeatsCount(request.getAvailableSeatsCount());
            if (request.getTotalSeats() != null) bus.setTotalSeats(request.getTotalSeats());
            System.out.println("bus service2= ");
            busRepository.save(bus);
            return Map.of("message", "Bus updated successfully");
        } catch (Exception e) {
            throw new RedBusException("BusService: ", "updateBus exception occurred", e);
        }
    }

   /* @Caching(evict = {
            @CacheEvict(value = "buses", allEntries = true),
            @CacheEvict(value = "availableSeats", key = "#busId")
    })*/
    // @CacheEvict(value = "bus_deleted", key = "#busId") // June 16
    @Caching(evict = {
            @CacheEvict(value = "buses", allEntries = true),
            @CacheEvict(value = "allBuses", allEntries = true),
            @CacheEvict(value = "availableSeats", key = "#busId")
    })
    public Map<String, String> deleteBus(Long busId) throws RedBusException {
        try {
            System.out.println("deleteBus service1 = " + LocalTime.now());
            Bus bus = busRepository.findById(busId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));
            busRepository.delete(bus);
            return Map.of("message", "Bus deleted successfully");
        } catch (Exception e) {
            throw new RedBusException("BusService: ", "deleteBus exception occurred", e);
        }
    }

    @Cacheable(value = "allBuses", key = "'allBuses'")
    public List<Bus> getAllBuses() throws RedBusException {
        try {
            System.out.println("getAllBuses service1 = " + LocalTime.now());
            return busRepository.findAll();
        } catch (Exception e) {
            throw new RedBusException("BusService: ", "getAllBuses exception occurred", e);
        }
    }
    /**
     * Generate consistent random seat numbers between 1-50 based on totalSeats
     * Same busId will always generate same seat numbers
     */
    /**
     * Add a new bus (your existing method)
     */
    // ── Bus Search ────────────────────────────────────────────────────────────
// June - 08
    @Cacheable(value = "buses", key = "#from + ':' + #to + ':' + #dateStr")
    public List<BusSearchResponse> searchBuses(String from, String to, String dateStr) throws RedBusException {
        try {
            System.out.println("BusService1 "+ LocalTime.now());
            LocalDate date = LocalDate.parse(dateStr);
            List<Bus> buses = busRepository
                    .findByFromLocationIgnoreCaseAndToLocationIgnoreCaseAndDateOfJourney(from, to, date);
            System.out.println("Buses " + buses);
            return buses.stream()
                    .map(bus -> {
                        // June -11
                        List<Integer> availableSeats = getAvailableSeatNumbers(bus.getBusId());

                        return BusSearchResponse.builder()
                                .busId(bus.getBusId())
                                .travelsName(bus.getTravelsName())
                                .availableSeats(availableSeats)
                                .totalSeats(bus.getTotalSeats())
                                .availableSeatsCount(availableSeats.size())
                                .fromLocation(bus.getFromLocation())
                                .toLocation(bus.getToLocation())
                                .dateOfJourney(bus.getDateOfJourney())
                                .price(bus.getPrice())
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RedBusException("BusService: ", "searchBuses exception occurred", e);
        }
    }

    // June - 11
    @Cacheable(value = "availableSeats", key = "#busId")
    public List<Integer> getAvailableSeatNumbers(Long busId) throws RedBusException {
        try {
            System.out.println("getAvailableSeatNumbers = " + LocalTime.now());
            // 1. Get the bus details
            Bus bus = busRepository.findById(busId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));

            // 2. Generate the base list of pseudo-random seat numbers
            List<Integer> busSeats = generateConsistentSeatNumbers(busId, bus.getAvailableSeatsCount());
            System.out.println("Base generated seats: " + busSeats);
            System.out.println("Seat Count from Bus Table: " + bus.getAvailableSeatsCount());

            // 3. Fetch all seat numbers that are already in the passenger table
            List<Integer> bookedSeats = passengerRepository.findBookedSeatNumbersByBusId(busId);
            System.out.println("Booked seats from passenger table: " + bookedSeats);

            // 4. Remove the booked seats so they become invisible to searching customers
            busSeats.removeAll(bookedSeats);

            System.out.println("Final available seat numbers = " + busSeats);
            return busSeats;
        } catch (Exception e) {
            throw new RedBusException("BusService: ", "getAvailableSeatNumbers exception occurred", e);
        }
    }

    private List<Integer> generateConsistentSeatNumbers(Long busId, Integer availableSeats) throws RedBusException {
        try {
            // Use busId as seed for consistent randomization
            System.out.println("availableSeats = " + availableSeats);
            Random random = new Random(busId);
            System.out.println("generateConsistentSeatNumbers1 Buse service ");
            // Create list of all possible seats (1 to 50)
            List<Integer> allPossibleSeats = IntStream.rangeClosed(1, 50)
                    .boxed()
                    .collect(Collectors.toList());
            System.out.println("generateConsistentSeatNumbers1 Buse service " + allPossibleSeats);
            // Shuffle using the seeded random
            Collections.shuffle(allPossibleSeats, random);


            // Take only the number of available seats specified by admin and sort them
            return allPossibleSeats.stream()
                    .limit(availableSeats)
                    .sorted()  // Sort for better user experience
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RedBusException("BusService: ", "generateConsistentSeatNumbers exception occurred", e);
        }
    }



}
