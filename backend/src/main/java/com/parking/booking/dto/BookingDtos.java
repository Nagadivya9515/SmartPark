package com.parking.booking.dto;

import com.parking.booking.model.Booking;
import java.util.List;

public class BookingDtos {

    // ── Create booking request ──────────────────────────────────────────────
    // Only the slot, the vehicle, and the intended duration come from the
    // client. Everything else (section/floor/lot names, the rate, the
    // vehicle's type) is resolved server-side from the real inventory and
    // the caller's own registered vehicle — the previous version accepted
    // all of that from the client and trusted it verbatim.
    public static class CreateBookingRequest {
        public Long slotId;
        public String vehicleNumber;
        public Integer durationHours;
    }

    // ── Booking response ────────────────────────────────────────────────────
    public static class BookingResponse {
        public Long id;
        public String bookingRef;
        public Long userId;
        public Long slotId;
        public String slotNumber;
        public String sectionName;
        public String floorName;
        public String lotName;
        public String lotAddress;
        public String vehicleNumber;
        public String vehicleLabel;
        public String vehicleType;
        public String entryDate;
        public String entryTime;
        public Integer durationHours;
        public Double ratePerHour;
        public Double serviceFee;
        public Double totalAmount;
        public String status;
        public String qrData;
        public String cancelledAt;
        public String createdAt;

        public static BookingResponse from(Booking b) {
            BookingResponse r = new BookingResponse();
            r.id            = b.getId();
            r.bookingRef    = b.getBookingRef();
            r.userId        = b.getUserId();
            r.slotId        = b.getSlotId();
            r.slotNumber    = b.getSlotNumber();
            r.sectionName   = b.getSectionName();
            r.floorName     = b.getFloorName();
            r.lotName       = b.getLotName();
            r.lotAddress    = b.getLotAddress();
            r.vehicleNumber = b.getVehicleNumber();
            r.vehicleLabel  = b.getVehicleLabel();
            r.vehicleType   = b.getVehicleType().name();
            r.entryDate     = b.getEntryDate().toString();
            r.entryTime     = b.getEntryTime().toString();
            r.durationHours = b.getDurationHours();
            r.ratePerHour   = b.getRatePerHour();
            r.serviceFee    = b.getServiceFee();
            r.totalAmount   = b.getTotalAmount();
            r.status        = b.getStatus().name();
            r.qrData        = b.getQrData();
            r.cancelledAt   = b.getCancelledAt() != null ? b.getCancelledAt().toString() : null;
            r.createdAt     = b.getCreatedAt().toString();
            return r;
        }
    }

    // ── My bookings summary ─────────────────────────────────────────────────
    public static class MyBookingsSummary {
        public long activeCount;
        public long totalCount;
        public double totalSpent;
        public List<BookingResponse> active;
        public List<BookingResponse> history;

        public MyBookingsSummary(long activeCount, long totalCount, double totalSpent,
                                  List<BookingResponse> active, List<BookingResponse> history) {
            this.activeCount = activeCount;
            this.totalCount  = totalCount;
            this.totalSpent  = totalSpent;
            this.active      = active;
            this.history     = history;
        }
    }

    // ── Vehicle DTO ──────────────────────────────────────────────────────────
    public static class VehicleDto {
        public Long id;
        public String vehicleNumber;
        public String vehicleLabel;
        public String vehicleType;

        public VehicleDto(Long id, String number, String label, String type) {
            this.id = id;
            this.vehicleNumber = number;
            this.vehicleLabel = label;
            this.vehicleType = type;
        }
    }
}
