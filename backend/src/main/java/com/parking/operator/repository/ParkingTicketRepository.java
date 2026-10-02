package com.parking.operator.repository;

import com.parking.operator.models.ParkingTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingTicketRepository extends JpaRepository<ParkingTicket, Long> {

    Optional<ParkingTicket> findByTicketId(String ticketId);

    Optional<ParkingTicket> findByVehicleNumberAndStatus(
            String vehicleNumber, ParkingTicket.TicketStatus status);

    @Query("SELECT COUNT(t) FROM ParkingTicket t WHERE t.entryDate = :date")
    long countByEntryDate(LocalDate date);

    @Query("SELECT COUNT(t) FROM ParkingTicket t WHERE t.status = :status")
    long countByStatus(ParkingTicket.TicketStatus status);

    @Query("SELECT COUNT(t) FROM ParkingTicket t WHERE t.exitDate = :date AND t.status = 'EXITED'")
    long countExitsByDate(LocalDate date);

    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM ParkingTicket t WHERE t.exitDate = :date AND t.status = 'EXITED'")
    double sumRevenueByDate(LocalDate date);

    @Query("SELECT COALESCE(AVG(t.durationHours), 0) FROM ParkingTicket t WHERE t.exitDate = :date AND t.status = 'EXITED'")
    double avgDurationByDate(LocalDate date);

    @Query("SELECT MAX(t.ticketId) FROM ParkingTicket t")
    Optional<String> findMaxTicketId();

    List<ParkingTicket> findByStatusOrderByCreatedAtDesc(ParkingTicket.TicketStatus status);
}