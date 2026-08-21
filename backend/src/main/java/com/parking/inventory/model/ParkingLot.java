package com.parking.inventory.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A physical parking facility. Single source of truth for "which lot" —
 * the dashboard (end-user view), the operator gate flow, and admin
 * reporting all read the same lot/slot rows (see {@link ParkingSlot}).
 * Previously the dashboard module and the operator module each seeded
 * and displayed their own unrelated lot.
 */
@Entity
@Table(name = "parking_lots")
public class ParkingLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;
    private Double rating;
    private Integer totalFloors;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public ParkingLot() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public Double getRating() { return rating; }
    public Integer getTotalFloors() { return totalFloors; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setRating(Double rating) { this.rating = rating; }
    public void setTotalFloors(Integer totalFloors) { this.totalFloors = totalFloors; }
}
