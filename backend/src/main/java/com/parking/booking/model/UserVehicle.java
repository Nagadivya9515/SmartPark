package com.parking.booking.model;

import com.parking.inventory.model.VehicleType;
import jakarta.persistence.*;

@Entity
@Table(name = "user_vehicles")
public class UserVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "vehicle_number", nullable = false)
    private String vehicleNumber;   // "ABC-1234"

    @Column(name = "vehicle_label")
    private String vehicleLabel;    // "Toyota Camry"

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    public UserVehicle() {}

    public Long getId()                { return id; }
    public Long getUserId()            { return userId; }
    public String getVehicleNumber()   { return vehicleNumber; }
    public String getVehicleLabel()    { return vehicleLabel; }
    public VehicleType getVehicleType(){ return vehicleType; }

    public void setId(Long id)                     { this.id = id; }
    public void setUserId(Long u)                  { this.userId = u; }
    public void setVehicleNumber(String v)         { this.vehicleNumber = v; }
    public void setVehicleLabel(String v)          { this.vehicleLabel = v; }
    public void setVehicleType(VehicleType t)      { this.vehicleType = t; }
}
