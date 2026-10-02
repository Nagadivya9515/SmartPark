package com.parking.operator.models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

import com.parking.inventory.model.VehicleType;

@Entity
@Table(name = "parking_tickets")
public class ParkingTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false, unique = true, length = 20)
    private String ticketId;

    @Column(name = "vehicle_number", nullable = false, length = 20)
    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "slot_number", nullable = false, length = 10)
    private String slotNumber;

    @Column(name = "section_name", length = 5)
    private String sectionName;

    @Column(name = "floor_name", length = 20)
    private String floorName;

    @Column(name = "gate_name", length = 20)
    private String gateName;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "entry_time", nullable = false)
    private LocalTime entryTime;

    @Column(name = "exit_date")
    private LocalDate exitDate;

    @Column(name = "exit_time")
    private LocalTime exitTime;

    @Column(name = "duration_hours")
    private Integer durationHours;

    @Column(name = "rate_per_hour")
    private Double ratePerHour;

    @Column(name = "parking_fee")
    private Double parkingFee;

    @Column(name = "service_fee")
    private Double serviceFee;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(name = "qr_data", length = 500)
    private String qrData;

    @Column(name = "operator_id")
    private String operatorId;

    @Column(name = "exit_operator_id")
    private String exitOperatorId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = TicketStatus.ACTIVE;
    }

    public enum TicketStatus { ACTIVE, EXITED, CANCELLED }

    public ParkingTicket() {}

    public Long getId()                   { return id; }
    public String getTicketId()           { return ticketId; }
    public String getVehicleNumber()      { return vehicleNumber; }
    public VehicleType getVehicleType()   { return vehicleType; }
    public String getSlotNumber()         { return slotNumber; }
    public String getSectionName()        { return sectionName; }
    public String getFloorName()          { return floorName; }
    public String getGateName()           { return gateName; }
    public LocalDate getEntryDate()       { return entryDate; }
    public LocalTime getEntryTime()       { return entryTime; }
    public LocalDate getExitDate()        { return exitDate; }
    public LocalTime getExitTime()        { return exitTime; }
    public Integer getDurationHours()     { return durationHours; }
    public Double getRatePerHour()        { return ratePerHour; }
    public Double getParkingFee()         { return parkingFee; }
    public Double getServiceFee()         { return serviceFee; }
    public Double getTotalAmount()        { return totalAmount; }
    public String getPaymentMethod()      { return paymentMethod; }
    public TicketStatus getStatus()       { return status; }
    public String getQrData()             { return qrData; }
    public String getOperatorId()           { return operatorId; }
    public String getExitOperatorId()       { return exitOperatorId; }
    public LocalDateTime getCreatedAt()   { return createdAt; }

    public void setId(Long id)                     { this.id = id; }
    public void setTicketId(String t)              { this.ticketId = t; }
    public void setVehicleNumber(String v)         { this.vehicleNumber = v; }
    public void setVehicleType(VehicleType t)      { this.vehicleType = t; }
    public void setSlotNumber(String s)            { this.slotNumber = s; }
    public void setSectionName(String s)           { this.sectionName = s; }
    public void setFloorName(String f)             { this.floorName = f; }
    public void setGateName(String g)              { this.gateName = g; }
    public void setEntryDate(LocalDate d)          { this.entryDate = d; }
    public void setEntryTime(LocalTime t)          { this.entryTime = t; }
    public void setExitDate(LocalDate d)           { this.exitDate = d; }
    public void setExitTime(LocalTime t)           { this.exitTime = t; }
    public void setDurationHours(Integer d)        { this.durationHours = d; }
    public void setRatePerHour(Double r)           { this.ratePerHour = r; }
    public void setParkingFee(Double f)            { this.parkingFee = f; }
    public void setServiceFee(Double f)            { this.serviceFee = f; }
    public void setTotalAmount(Double t)           { this.totalAmount = t; }
    public void setPaymentMethod(String m)         { this.paymentMethod = m; }
    public void setStatus(TicketStatus s)          { this.status = s; }
    public void setQrData(String q)                { this.qrData = q; }
    public void setOperatorId(String o)              { this.operatorId = o; }
    public void setExitOperatorId(String o)          { this.exitOperatorId = o; }
}