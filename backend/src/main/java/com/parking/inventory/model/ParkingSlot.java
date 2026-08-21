package com.parking.inventory.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A single physical parking slot. This is the one slot table in the system —
 * it replaces the formerly separate dashboard.ParkingSlot (behind a
 * ParkingLot/ParkingSection hierarchy) and operator.OperatorSlot (flat, with
 * a ticket pointer), which described two different, unsynchronized
 * inventories. The end-user dashboard, the operator entry/exit flow, and
 * end-user bookings now all read and write this same table, so "occupied"
 * means the same thing everywhere: a slot someone has booked ahead of time
 * and a slot a car is physically parked in are the same flag.
 */
@Entity
@Table(name = "parking_slots")
public class ParkingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private ParkingLot lot;

    @Column(name = "slot_number", nullable = false, length = 10)
    private String slotNumber;      // "01", "02" ...

    @Column(name = "display_number", nullable = false, length = 10)
    private String displayNumber;   // "A-01"

    @Column(name = "section_name", nullable = false, length = 5)
    private String sectionName;     // "A", "B" ...

    @Column(name = "floor_name", nullable = false, length = 20)
    private String floorName;       // "Floor 1", "Floor 2"

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false)
    private Boolean occupied = false;

    @Column(name = "occupied_since")
    private LocalDateTime occupiedSince;

    @Column(name = "current_ticket_id", length = 20)
    private String currentTicketId;

    @Column(name = "rate_per_hour")
    private Double ratePerHour;

    public ParkingSlot() {}

    public Long getId() { return id; }
    public ParkingLot getLot() { return lot; }
    public String getSlotNumber() { return slotNumber; }
    public String getDisplayNumber() { return displayNumber; }
    public String getSectionName() { return sectionName; }
    public String getFloorName() { return floorName; }
    public VehicleType getVehicleType() { return vehicleType; }
    public Boolean getOccupied() { return occupied; }
    public LocalDateTime getOccupiedSince() { return occupiedSince; }
    public String getCurrentTicketId() { return currentTicketId; }
    public Double getRatePerHour() { return ratePerHour; }

    public void setId(Long id) { this.id = id; }
    public void setLot(ParkingLot lot) { this.lot = lot; }
    public void setSlotNumber(String slotNumber) { this.slotNumber = slotNumber; }
    public void setDisplayNumber(String displayNumber) { this.displayNumber = displayNumber; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }
    public void setFloorName(String floorName) { this.floorName = floorName; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }
    public void setCurrentTicketId(String currentTicketId) { this.currentTicketId = currentTicketId; }
    public void setRatePerHour(Double ratePerHour) { this.ratePerHour = ratePerHour; }

    public void setOccupied(Boolean occupied) {
        this.occupied = occupied;
        this.occupiedSince = Boolean.TRUE.equals(occupied) ? LocalDateTime.now() : null;
    }
}
