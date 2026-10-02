package com.parking.dashboard.dto;

import java.util.List;

public class DashboardDto {

    // ── Top summary ────────────────────────────────────────────────────────
    public static class SummaryDto {
        public long availableSlots;
        public long totalCapacity;
        public double occupancyRate;
        public int parkingFloors;
        public String lotName;
        public String address;
        public double rating;

        public SummaryDto(long available, long total, int floors,
                          String name, String address, double rating) {
            this.availableSlots  = available;
            this.totalCapacity   = total;
            this.occupancyRate   = total > 0
                    ? Math.round(((double)(total - available) / total) * 100.0) : 0;
            this.parkingFloors   = floors;
            this.lotName         = name;
            this.address         = address;
            this.rating          = rating;
        }
    }

    // ── Per-type summary (Car/Bike/Truck) ──────────────────────────────────
    public static class VehicleTypeDto {
        public String type;
        public long available;
        public long total;

        public VehicleTypeDto(String type, long available, long total) {
            this.type      = type;
            this.available = available;
            this.total     = total;
        }
    }

    // ── Slot ───────────────────────────────────────────────────────────────
    public static class SlotDto {
        public Long id;
        public String slotNumber;
        public boolean occupied;

        public SlotDto(Long id, String slotNumber, boolean occupied) {
            this.id         = id;
            this.slotNumber = slotNumber;
            this.occupied   = occupied;
        }
    }

    // ── Section (a floor/vehicle-type grouping of slots — no longer a
    //    separate DB row; derived on the fly from ParkingSlot) ─────────────
    public static class SectionDto {
        public Long id;
        public String sectionName;
        public String floor;
        public String vehicleType;
        public long available;
        public long total;
        public List<SlotDto> slots;

        public SectionDto(Long id, String sectionName, String floor, String vehicleType,
                          long available, long total, List<SlotDto> slots) {
            this.id          = id;
            this.sectionName = sectionName;
            this.floor       = floor;
            this.vehicleType = vehicleType;
            this.available   = available;
            this.total       = total;
            this.slots       = slots;
        }
    }

    // ── Rates ──────────────────────────────────────────────────────────────
    public static class RateDto {
        public String vehicleType;
        public double ratePerHour;

        public RateDto(String vehicleType, double ratePerHour) {
            this.vehicleType  = vehicleType;
            this.ratePerHour  = ratePerHour;
        }
    }

    // ── Full dashboard response ────────────────────────────────────────────
    public static class DashboardResponse {
        public SummaryDto summary;
        public List<VehicleTypeDto> vehicleTypeSummary;
        public List<SectionDto> sections;
        public List<RateDto> rates;

        public DashboardResponse(SummaryDto summary,
                                  List<VehicleTypeDto> vehicleTypeSummary,
                                  List<SectionDto> sections,
                                  List<RateDto> rates) {
            this.summary            = summary;
            this.vehicleTypeSummary = vehicleTypeSummary;
            this.sections           = sections;
            this.rates              = rates;
        }
    }

    // ── Single slot detail — used by the booking page when a user lands on
    //    /booking/:id directly (no dashboard router state to fall back on).
    //    Previously the frontend called a bookings endpoint that never
    //    existed on the backend and always 404'd. ─────────────────────────
    public static class SlotDetailDto {
        public Long id;
        public String slotNumber;
        public String displayNumber;
        public String sectionName;
        public String floorName;
        public String vehicleType;
        public boolean occupied;
        public Double ratePerHour;
        public String lotName;
        public String lotAddress;

        public SlotDetailDto(Long id, String slotNumber, String displayNumber,
                              String sectionName, String floorName, String vehicleType,
                              boolean occupied, Double ratePerHour,
                              String lotName, String lotAddress) {
            this.id            = id;
            this.slotNumber    = slotNumber;
            this.displayNumber = displayNumber;
            this.sectionName   = sectionName;
            this.floorName     = floorName;
            this.vehicleType   = vehicleType;
            this.occupied      = occupied;
            this.ratePerHour   = ratePerHour;
            this.lotName       = lotName;
            this.lotAddress    = lotAddress;
        }
    }
}
