package com.parking.booking.controller;

import com.parking.auth.repository.UserRepository;
import com.parking.booking.dto.BookingDtos;
import com.parking.booking.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public BookingController(BookingService bookingService, UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    // POST /api/bookings — create a booking for the logged-in user.
    // Previously always booked as a hardcoded demo user id (1L), regardless
    // of who was actually logged in.
    @PostMapping
    public ResponseEntity<BookingDtos.BookingResponse> create(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody BookingDtos.CreateBookingRequest req) {
        return ResponseEntity.ok(bookingService.createBooking(currentUserId(principal), req));
    }

    // GET /api/bookings/{ref} — get one booking by ref
    @GetMapping("/{ref}")
    public ResponseEntity<BookingDtos.BookingResponse> getOne(@PathVariable String ref) {
        return ResponseEntity.ok(bookingService.getBooking(ref));
    }

    // GET /api/bookings/vehicles — the logged-in user's saved vehicles
    @GetMapping("/vehicles")
    public ResponseEntity<List<BookingDtos.VehicleDto>> vehicles(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(bookingService.getVehicles(currentUserId(principal)));
    }

    // GET /api/bookings/my — all bookings for the logged-in user
    @GetMapping("/my")
    public ResponseEntity<BookingDtos.MyBookingsSummary> myBookings(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(bookingService.getMyBookings(currentUserId(principal)));
    }

    // PATCH /api/bookings/{ref}/cancel
    @PatchMapping("/{ref}/cancel")
    public ResponseEntity<BookingDtos.BookingResponse> cancel(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String ref) {
        return ResponseEntity.ok(bookingService.cancelBooking(ref, currentUserId(principal)));
    }

    private Long currentUserId(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + principal.getUsername()))
                .getId();
    }
}
