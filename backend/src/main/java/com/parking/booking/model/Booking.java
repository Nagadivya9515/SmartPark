package com.parking.booking.model;

import com.parking.inventory.model.ParkingSlot;
import com.parking.inventory.model.VehicleType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_ref", nullable = false, unique = true, length = 20)
    private String bookingRef;          // BK001, BK002 …

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // The real slot this booking holds. Booking, the operator gate flow, and
    // the dashboard now all point at the same ParkingSlot row — previously
    // a booking only carried a free-floating slotId with no foreign key,
    // and the slot numbers/lot it described lived in a completely separate
    // (and separately seeded) inventory.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private ParkingSlot slot;

    // Denormalised snapshot of the slot/lot at booking time, for display on
    // receipts even if the slot's own numbering changes later. Populated by
    // the server from the resolved slot — never trusted from the client.
    @Column(name = "slot_number", nullable = false)
    private String slotNumber;          // "A-15"

    @Column(name = "section_name", nullable = false)
    private String sectionName;

    @Column(name = "floor_name", nullable = false)
    private String floorName;

    @Column(name = "lot_name", nullable = false)
    private String lotName;

    @Column(name = "lot_address")
    private String lotAddress;

    // Vehicle
    @Column(name = "vehicle_number", nullable = false)
    private String vehicleNumber;       // "ABC-1234"

    @Column(name = "vehicle_label")
    private String vehicleLabel;        // "Toyota Camry"

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    // Timing
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "entry_time", nullable = false)
    private LocalTime entryTime;

    @Column(name = "duration_hours", nullable = false)
    private Integer durationHours;

    // Pricing
    @Column(name = "rate_per_hour", nullable = false)
    private Double ratePerHour;

    @Column(name = "service_fee", nullable = false)
    private Double serviceFee;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "qr_data")
    private String qrData;              // JSON string encoded in QR

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = BookingStatus.ACTIVE;
    }

    public enum BookingStatus { ACTIVE, COMPLETED, CANCELLED }

    public Booking() {}

    // ── Getters ────────────────────────────────────────────────────────────
    public Long getId()               { return id; }
    public String getBookingRef()     { return bookingRef; }
    public Long getUserId()           { return userId; }
    public ParkingSlot getSlot()      { return slot; }
    public Long getSlotId()           { return slot != null ? slot.getId() : null; }
    public String getSlotNumber()     { return slotNumber; }
    public String getSectionName()    { return sectionName; }
    public String getFloorName()      { return floorName; }
    public String getLotName()        { return lotName; }
    public String getLotAddress()     { return lotAddress; }
    public String getVehicleNumber()  { return vehicleNumber; }
    public String getVehicleLabel()   { return vehicleLabel; }
    public VehicleType getVehicleType()   { return vehicleType; }
    public LocalDate getEntryDate()   { return entryDate; }
    public LocalTime getEntryTime()   { return entryTime; }
    public Integer getDurationHours() { return durationHours; }
    public Double getRatePerHour()    { return ratePerHour; }
    public Double getServiceFee()     { return serviceFee; }
    public Double getTotalAmount()    { return totalAmount; }
    public BookingStatus getStatus()  { return status; }
    public String getQrData()         { return qrData; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }

    // ── Setters ────────────────────────────────────────────────────────────
    public void setId(Long id)                    { this.id = id; }
    public void setBookingRef(String r)           { this.bookingRef = r; }
    public void setUserId(Long u)                 { this.userId = u; }
    public void setSlot(ParkingSlot s)            { this.slot = s; }
    public void setSlotNumber(String s)           { this.slotNumber = s; }
    public void setSectionName(String s)          { this.sectionName = s; }
    public void setFloorName(String f)            { this.floorName = f; }
    public void setLotName(String l)              { this.lotName = l; }
    public void setLotAddress(String a)           { this.lotAddress = a; }
    public void setVehicleNumber(String v)        { this.vehicleNumber = v; }
    public void setVehicleLabel(String v)         { this.vehicleLabel = v; }
    public void setVehicleType(VehicleType t)     { this.vehicleType = t; }
    public void setEntryDate(LocalDate d)         { this.entryDate = d; }
    public void setEntryTime(LocalTime t)         { this.entryTime = t; }
    public void setDurationHours(Integer d)       { this.durationHours = d; }
    public void setRatePerHour(Double r)          { this.ratePerHour = r; }
    public void setServiceFee(Double f)           { this.serviceFee = f; }
    public void setTotalAmount(Double t)          { this.totalAmount = t; }
    public void setStatus(BookingStatus s)        { this.status = s; }
    public void setQrData(String q)               { this.qrData = q; }
    public void setCancelledAt(LocalDateTime c)   { this.cancelledAt = c; }
}
