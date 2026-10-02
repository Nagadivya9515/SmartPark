package com.parking.inventory.repository;

import com.parking.inventory.model.ParkingSlot;
import com.parking.inventory.model.VehicleType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {

    List<ParkingSlot> findByLotId(Long lotId);

    List<ParkingSlot> findAllByOrderByFloorNameAscSectionNameAscSlotNumberAsc();

    @Query("SELECT COUNT(s) FROM ParkingSlot s WHERE s.lot.id = :lotId AND s.occupied = false")
    long countAvailableByLotId(Long lotId);

    @Query("SELECT COUNT(s) FROM ParkingSlot s WHERE s.lot.id = :lotId")
    long countTotalByLotId(Long lotId);

    @Query("SELECT COUNT(s) FROM ParkingSlot s WHERE s.lot.id = :lotId AND s.sectionName = :section AND s.occupied = false")
    long countAvailableByLotIdAndSection(Long lotId, String section);

    @Query("SELECT COUNT(s) FROM ParkingSlot s WHERE s.occupied = false")
    long countAvailable();

    @Query("SELECT COUNT(s) FROM ParkingSlot s WHERE s.vehicleType = :type AND s.occupied = false")
    long countAvailableByType(VehicleType type);

    /**
     * Locks the first available slot of the requested type for the duration
     * of the caller's transaction, so two entry/booking requests racing for
     * the last slot of a type can't both succeed. (Previously entry ticket
     * generation used a plain, unlocked SELECT here.)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ParkingSlot s WHERE s.vehicleType = :type AND s.occupied = false ORDER BY s.sectionName ASC, s.slotNumber ASC")
    List<ParkingSlot> findAvailableByVehicleTypeWithLock(VehicleType type);

    List<ParkingSlot> findByCurrentTicketId(String ticketId);
}
