package com.parking.booking;

import com.parking.ConcurrencyTestHelper;
import com.parking.IntegrationTestBase;
import com.parking.TestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for concurrent booking scenarios.
 * Tests pessimistic locking on ParkingSlot to prevent double-booking.
 * 
 * SCENARIO: Multiple users attempt to book the same slot simultaneously.
 * EXPECTED: First succeeds (200/201), subsequent requests fail with 409 Conflict,
 *           data integrity is maintained.
 */
@DisplayName("Booking Concurrency Integration Tests")
public class BookingConcurrencyIT extends IntegrationTestBase {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ParkingSlotRepository parkingSlotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Long availableSlotId;
    private String userEmail;
    private Long userId;

    @BeforeEach
    @Transactional
    void setUp() {
        // Create parking lot
        ParkingLot lot = new ParkingLot();
        lot.setName("Test Lot");
        lot.setLocation("Test Location");
        ParkingLot savedLot = parkingLotRepository.save(lot);

        // Create test user
        User user = new User();
        user.setEmail(TestSupport.TestData.USER_EMAIL);
        user.setPassword("encoded_password");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(User.Role.USER);
        User savedUser = userRepository.save(user);
        userId = savedUser.getId();
        userEmail = savedUser.getEmail();

        // Create test vehicle
        Vehicle vehicle = new Vehicle();
        vehicle.setUser(savedUser);
        vehicle.setPlateNumber(TestSupport.TestData.USER_VEHICLE_PLATE);
        vehicle.setVehicleType("CAR");
        vehicleRepository.save(vehicle);

        // Create available parking slot
        ParkingSlot slot = new ParkingSlot();
        slot.setParkingLot(savedLot);
        slot.setSlotNumber(TestSupport.TestData.TEST_SLOT_NUMBER);
        slot.setSlotSection("A");
        slot.setFloor(1);
        slot.setIsOccupied(false);
        slot.setHourlyRate(5.0);
        ParkingSlot savedSlot = parkingSlotRepository.save(slot);
        availableSlotId = savedSlot.getId();
    }

    @Test
    @DisplayName("Should allow first booking and reject subsequent concurrent attempts with 409 Conflict")
    void testConcurrentBookingAttemptsOnSameSlot() throws Exception {
        int threadCount = 5;
        List<Callable<Integer>> concurrentBookingTasks = new ArrayList<>();

        // Create 5 concurrent booking requests on the same slot
        for (int i = 0; i < threadCount; i++) {
            concurrentBookingTasks.add(() -> {
                try {
                    String requestBody = objectMapper.writeValueAsString(
                        new BookingRequest(availableSlotId, TestSupport.TestData.USER_VEHICLE_PLATE, 60)
                    );
                    
                    MvcResult result = mockMvc.perform(
                        post("/api/parking/book")
                            .contentType("application/json")
                            .content(requestBody)
                            .with(user(userEmail).roles("USER"))
                    ).andReturn();
                    return result.getResponse().getStatus();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // Execute all tasks concurrently
        List<Integer> statusCodes = ConcurrencyTestHelper.executeTasksConcurrently(concurrentBookingTasks);

        // Assertions
        long successCount = statusCodes.stream().filter(s -> s == 201 || s == 200).count();
        long conflictCount = statusCodes.stream().filter(s -> s == 409).count();

        assertThat(successCount)
            .as("Exactly one booking should succeed")
            .isEqualTo(1);

        assertThat(conflictCount)
            .as("Four bookings should fail with 409 Conflict")
            .isEqualTo(4);
    }

    @Test
    @DisplayName("Should maintain slot occupancy consistency after concurrent bookings")
    void testSlotOccupancyConsistencyAfterConcurrency() throws Exception {
        int threadCount = 3;
        
        // Attempt concurrent bookings
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                try {
                    String requestBody = objectMapper.writeValueAsString(
                        new BookingRequest(availableSlotId, TestSupport.TestData.USER_VEHICLE_PLATE, 60)
                    );
                    
                    MvcResult result = mockMvc.perform(
                        post("/api/parking/book")
                            .contentType("application/json")
                            .content(requestBody)
                            .with(user(userEmail).roles("USER"))
                    ).andReturn();
                    return result.getResponse().getStatus();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        
        ConcurrencyTestHelper.executeTasksConcurrently(tasks);

        // Verify slot state: exactly one booking exists
        ParkingSlot slot = parkingSlotRepository.findById(availableSlotId).orElseThrow();
        assertThat(slot.getIsOccupied())
            .as("Slot should be marked as occupied after successful booking")
            .isTrue();
        
        long activeBookingCount = bookingRepository.countBySlotAndStatusNotCancelled(slot);
        assertThat(activeBookingCount)
            .as("Exactly one booking should exist for the slot")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("Should not corrupt data when multiple threads fail to book")
    void testDataIntegrityUnderConcurrentFailure() throws Exception {
        // Book the slot first
        Booking initialBooking = new Booking();
        initialBooking.setUser(userRepository.findById(userId).orElseThrow());
        initialBooking.setSlot(parkingSlotRepository.findById(availableSlotId).orElseThrow());
        initialBooking.setVehicle(vehicleRepository.findByPlateNumber(TestSupport.TestData.USER_VEHICLE_PLATE).orElseThrow());
        initialBooking.setStatus("ACTIVE");
        bookingRepository.save(initialBooking);

        // Mark slot as occupied
        ParkingSlot slot = parkingSlotRepository.findById(availableSlotId).orElseThrow();
        slot.setIsOccupied(true);
        parkingSlotRepository.save(slot);

        // Now 3 threads attempt to book the already-occupied slot
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            tasks.add(() -> {
                try {
                    String requestBody = objectMapper.writeValueAsString(
                        new BookingRequest(availableSlotId, TestSupport.TestData.USER_VEHICLE_PLATE, 60)
                    );
                    
                    MvcResult result = mockMvc.perform(
                        post("/api/parking/book")
                            .contentType("application/json")
                            .content(requestBody)
                            .with(user(userEmail).roles("USER"))
                    ).andReturn();
                    return result.getResponse().getStatus();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        List<Integer> statuses = ConcurrencyTestHelper.executeTasksConcurrently(tasks);

        // All should fail with 409
        assertThat(statuses).allMatch(status -> status == 409);
        
        // Verify original booking still intact
        long bookingCount = bookingRepository.countBySlot(slot);
        assertThat(bookingCount)
            .as("Only the original booking should exist")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle 10 concurrent booking attempts on single slot")
    void testHighConcurrencyBookingAttempts() throws Exception {
        int threadCount = 10;
        List<Callable<Integer>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                try {
                    String requestBody = objectMapper.writeValueAsString(
                        new BookingRequest(availableSlotId, TestSupport.TestData.USER_VEHICLE_PLATE, 60)
                    );
                    
                    MvcResult result = mockMvc.perform(
                        post("/api/parking/book")
                            .contentType("application/json")
                            .content(requestBody)
                            .with(user(userEmail).roles("USER"))
                    ).andReturn();
                    return result.getResponse().getStatus();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        List<Integer> statuses = ConcurrencyTestHelper.executeTasksConcurrently(tasks);

        // Exactly 1 success, 9 conflicts
        assertThat(statuses.stream().filter(s -> s == 201).count())
            .as("Exactly one booking should succeed under high concurrency")
            .isEqualTo(1);
        
        assertThat(statuses.stream().filter(s -> s == 409).count())
            .as("Nine bookings should fail with 409 Conflict")
            .isEqualTo(9);
    }

    @Test
    @DisplayName("Should reject concurrent bookings for non-existent slot")
    void testConcurrentBookingOfNonExistentSlot() throws Exception {
        Long nonExistentSlotId = 99999L;
        
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            tasks.add(() -> {
                try {
                    String requestBody = objectMapper.writeValueAsString(
                        new BookingRequest(nonExistentSlotId, TestSupport.TestData.USER_VEHICLE_PLATE, 60)
                    );
                    
                    MvcResult result = mockMvc.perform(
                        post("/api/parking/book")
                            .contentType("application/json")
                            .content(requestBody)
                            .with(user(userEmail).roles("USER"))
                    ).andReturn();
                    return result.getResponse().getStatus();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        List<Integer> statuses = ConcurrencyTestHelper.executeTasksConcurrently(tasks);

        // All should fail with 404 or 400
        assertThat(statuses).allMatch(status -> status == 404 || status == 400);
    }

    /**
     * DTO for booking requests
     */
    static class BookingRequest {
        public Long slotId;
        public String vehiclePlate;
        public int durationMinutes;

        BookingRequest(Long slotId, String vehiclePlate, int durationMinutes) {
            this.slotId = slotId;
            this.vehiclePlate = vehiclePlate;
            this.durationMinutes = durationMinutes;
        }
    }
}
