package com.parking.booking.repository;

import com.parking.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Booking> findByBookingRef(String bookingRef);

    List<Booking> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Booking.BookingStatus status);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
           "WHERE b.slot.id = :slotId AND b.status = :status")
    boolean existsBySlotIdAndStatus(@Param("slotId") Long slotId, @Param("status") Booking.BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.userId = :userId AND b.status = 'ACTIVE'")
    long countActiveByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.userId = :userId AND b.status != 'CANCELLED'")
    double sumTotalByUserId(Long userId);

    @Query("SELECT MAX(b.bookingRef) FROM Booking b")
    Optional<String> findMaxBookingRef();
}
