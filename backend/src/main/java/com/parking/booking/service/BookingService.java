package com.parking.booking.service;

import com.parking.booking.dto.BookingDtos;
import com.parking.booking.model.Booking;
import com.parking.booking.model.UserVehicle;
import com.parking.booking.repository.BookingRepository;
import com.parking.booking.repository.UserVehicleRepository;
import com.parking.inventory.model.ParkingSlot;
import com.parking.inventory.repository.ParkingSlotRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private static final double SERVICE_FEE = 1.50;

    private final BookingRepository     bookingRepo;
    private final UserVehicleRepository vehicleRepo;
    private final ParkingSlotRepository slotRepo;

    public BookingService(BookingRepository bookingRepo,
                           UserVehicleRepository vehicleRepo,
                           ParkingSlotRepository slotRepo) {
        this.bookingRepo = bookingRepo;
        this.vehicleRepo = vehicleRepo;
        this.slotRepo    = slotRepo;
    }

    // ── Create booking ──────────────────────────────────────────────────────
    // The slot, its lot, its rate, and the vehicle's type all come from the
    // database now, not from whatever the client sends — the previous
    // version trusted slot number/section/floor/lot/rate as freeform client
    // fields with no link back to real inventory.
    @Transactional
    public BookingDtos.BookingResponse createBooking(Long userId, BookingDtos.CreateBookingRequest req) {
        ParkingSlot slot = slotRepo.findById(req.slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + req.slotId));

        if (Boolean.TRUE.equals(slot.getOccupied())) {
            throw new RuntimeException("Slot already booked");
        }

        UserVehicle vehicle = vehicleRepo.findByUserId(userId).stream()
                .filter(v -> v.getVehicleNumber().equalsIgnoreCase(req.vehicleNumber))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (vehicle.getVehicleType() != slot.getVehicleType()) {
            throw new RuntimeException("Vehicle type mismatch with selected slot!");
        }

        int hours     = req.durationHours != null && req.durationHours > 0 ? req.durationHours : 1;
        double rate   = slot.getRatePerHour() != null ? slot.getRatePerHour() : 0.0;
        double parking = rate * hours;
        double total   = parking + SERVICE_FEE;

        Booking b = new Booking();
        b.setBookingRef(generateRef());
        b.setUserId(userId);
        b.setSlot(slot);
        b.setSlotNumber(slot.getDisplayNumber());
        b.setSectionName(slot.getSectionName());
        b.setFloorName(slot.getFloorName());
        b.setLotName(slot.getLot().getName());
        b.setLotAddress(slot.getLot().getAddress());
        b.setVehicleNumber(vehicle.getVehicleNumber());
        b.setVehicleLabel(vehicle.getVehicleLabel());
        b.setVehicleType(vehicle.getVehicleType());
        b.setEntryDate(LocalDate.now());
        b.setEntryTime(LocalTime.now().withNano(0));
        b.setDurationHours(hours);
        b.setRatePerHour(rate);
        b.setServiceFee(SERVICE_FEE);
        b.setTotalAmount(total);
        b.setStatus(Booking.BookingStatus.ACTIVE);

        // Reserving the slot is what makes this booking real: the same
        // occupied flag the dashboard and the operator gate flow read.
        slot.setOccupied(true);
        slotRepo.save(slot);

        b.setQrData(buildQrData(b));
        bookingRepo.save(b);

        return BookingDtos.BookingResponse.from(b);
    }

    // ── Get single booking ──────────────────────────────────────────────────
    public BookingDtos.BookingResponse getBooking(String ref) {
        Booking b = bookingRepo.findByBookingRef(ref)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + ref));
        return BookingDtos.BookingResponse.from(b);
    }

    // ── My bookings ──────────────────────────────────────────────────────────
    public BookingDtos.MyBookingsSummary getMyBookings(Long userId) {
        List<Booking> all = bookingRepo.findByUserIdOrderByCreatedAtDesc(userId);

        List<BookingDtos.BookingResponse> active = all.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.ACTIVE)
                .map(BookingDtos.BookingResponse::from).collect(Collectors.toList());

        List<BookingDtos.BookingResponse> history = all.stream()
                .filter(b -> b.getStatus() != Booking.BookingStatus.ACTIVE)
                .map(BookingDtos.BookingResponse::from).collect(Collectors.toList());

        long activeCount = bookingRepo.countActiveByUserId(userId);
        double totalSpent = bookingRepo.sumTotalByUserId(userId);

        return new BookingDtos.MyBookingsSummary(
                activeCount, all.size(), totalSpent, active, history);
    }

    // ── Cancel booking ───────────────────────────────────────────────────────
    @Transactional
    public BookingDtos.BookingResponse cancelBooking(String ref, Long userId) {
        Booking b = bookingRepo.findByBookingRef(ref)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if (!b.getUserId().equals(userId))
            throw new RuntimeException("Not authorized");
        if (b.getStatus() != Booking.BookingStatus.ACTIVE)
            throw new RuntimeException("Booking is not active");

        b.setStatus(Booking.BookingStatus.CANCELLED);
        b.setCancelledAt(LocalDateTime.now());
        bookingRepo.save(b);

        // Free the slot back up for booking or gate entry.
        ParkingSlot slot = b.getSlot();
        slot.setOccupied(false);
        slotRepo.save(slot);

        return BookingDtos.BookingResponse.from(b);
    }

    // ── User vehicles ────────────────────────────────────────────────────────
    public List<BookingDtos.VehicleDto> getVehicles(Long userId) {
        return vehicleRepo.findByUserId(userId).stream()
                .map(v -> new BookingDtos.VehicleDto(
                        v.getId(), v.getVehicleNumber(),
                        v.getVehicleLabel(), v.getVehicleType().name()))
                .collect(Collectors.toList());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private String generateRef() {
        String max = bookingRepo.findMaxBookingRef().orElse("BK000");
        int num = Integer.parseInt(max.substring(2)) + 1;
        return String.format("BK%03d", num);
    }

    private String buildQrData(Booking b) {
        return String.format(
            "{\"ref\":\"%s\",\"slot\":\"%s\",\"vehicle\":\"%s\",\"date\":\"%s\",\"time\":\"%s\"}",
            b.getBookingRef(), b.getSlotNumber(),
            b.getVehicleNumber(), b.getEntryDate(), b.getEntryTime());
    }
}
