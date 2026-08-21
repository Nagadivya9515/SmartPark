package com.parking.operator.dto;

import com.parking.inventory.model.ParkingSlot;
import com.parking.operator.models.Operator;
import com.parking.operator.models.ParkingTicket;

import java.util.List;
import java.util.Map;

public class OperatorDtos {

    // ── Auth ───────────────────────────────────────────────────────────────
    public static class LoginRequest {
        public String operatorId;
        public String password;
        public boolean rememberMe;
    }

    public static class LoginResponse {
        public String token;
        public String operatorId;
        public String fullName;
        public String gateName;
        public String lotName;
        public String role;
        public String message;

        public LoginResponse(String token, Operator op) {
            this.token      = token;
            this.operatorId = op.getOperatorId();
            this.fullName   = op.getFullName();
            this.gateName   = op.getGateName();
            this.lotName    = op.getLotName();
            this.role       = op.getRole().name();
            this.message    = "Login successful";
        }
    }

    // ── Entry ──────────────────────────────────────────────────────────────
    public static class EntryRequest {
        public String vehicleNumber;
        public String vehicleType;
        public String gateName;
        public String operatorId; // OP001, OP002
    }

    // ── Ticket ─────────────────────────────────────────────────────────────
    public static class TicketResponse {
        public Long   id;
        public String ticketId;
        public String vehicleNumber;
        public String vehicleType;
        public String slotNumber;
        public String sectionName;
        public String floorName;
        public String gateName;
        public String entryDate;
        public String entryTime;
        public String exitDate;
        public String exitTime;
        public Integer durationHours;
        public Double ratePerHour;
        public Double parkingFee;
        public Double serviceFee;
        public Double totalAmount;
        public String paymentMethod;
        public String status;
        public String qrData;

        public static TicketResponse from(ParkingTicket t) {
            TicketResponse r   = new TicketResponse();
            r.id               = t.getId();
            r.ticketId         = t.getTicketId();
            r.vehicleNumber    = t.getVehicleNumber();
            r.vehicleType      = t.getVehicleType().name();
            r.slotNumber       = t.getSlotNumber();
            r.sectionName      = t.getSectionName();
            r.floorName        = t.getFloorName();
            r.gateName         = t.getGateName();
            r.entryDate        = t.getEntryDate() != null ? t.getEntryDate().toString() : null;
            r.entryTime        = t.getEntryTime() != null ? t.getEntryTime().toString() : null;
            r.exitDate         = t.getExitDate()  != null ? t.getExitDate().toString()  : null;
            r.exitTime         = t.getExitTime()  != null ? t.getExitTime().toString()  : null;
            r.durationHours    = t.getDurationHours();
            r.ratePerHour      = t.getRatePerHour();
            r.parkingFee       = t.getParkingFee();
            r.serviceFee       = t.getServiceFee();
            r.totalAmount      = t.getTotalAmount();
            r.paymentMethod    = t.getPaymentMethod();
            r.status           = t.getStatus().name();
            r.qrData           = t.getQrData();
            return r;
        }
    }

    // ── Exit request ───────────────────────────────────────────────────────
    public static class ExitRequest {
        public String ticketId;
        public String paymentMethod;   // CASH | CARD | UPI
        public String operatorId;      // OP001, OP002
    }

    // ── Slot DTO ───────────────────────────────────────────────────────────
    public static class SlotDto {
        public Long    id;
        public String  slotNumber;
        public String  displayNumber;
        public String  sectionName;
        public String  floorName;
        public String  vehicleType;
        public boolean occupied;
        public String  currentTicketId;

        public static SlotDto from(ParkingSlot s) {
            SlotDto d         = new SlotDto();
            d.id              = s.getId();
            d.slotNumber      = s.getSlotNumber();
            d.displayNumber   = s.getDisplayNumber();
            d.sectionName     = s.getSectionName();
            d.floorName       = s.getFloorName();
            d.vehicleType     = s.getVehicleType().name();
            d.occupied        = s.getOccupied();
            d.currentTicketId = s.getCurrentTicketId();
            return d;
        }
    }

    // ── Section ────────────────────────────────────────────────────────────
    public static class SectionDto {
        public String        sectionName;
        public String        vehicleType;
        public long          available;
        public long          occupied;
        public List<SlotDto> slots;

        public SectionDto(String sec, String type, long avail, long occ, List<SlotDto> slots) {
            this.sectionName = sec;
            this.vehicleType = type;
            this.available   = avail;
            this.occupied    = occ;
            this.slots       = slots;
        }
    }

    // ── Floor ──────────────────────────────────────────────────────────────
    public static class FloorDto {
        public String           floorName;
        public long             totalSlots;
        public long             availableSlots;
        public long             occupiedSlots;
        public double           occupancyRate;
        public List<SectionDto> sections;

        public FloorDto(String floor, long total, long avail, List<SectionDto> sections) {
            this.floorName      = floor;
            this.totalSlots     = total;
            this.availableSlots = avail;
            this.occupiedSlots  = total - avail;
            this.occupancyRate  = total > 0
                ? Math.round(((double)(total - avail) / total) * 100.0) : 0;
            this.sections       = sections;
        }
    }

    // ── Live status ────────────────────────────────────────────────────────
    public static class LiveStatusResponse {
        public long                totalSlots;
        public long                occupied;
        public long                available;
        public double              occupancyRate;
        public long                todayEntries;
        public Map<String, Long>   availableByType;
        public List<FloorDto>      floors;

        public LiveStatusResponse(long total, long occ, long todayEntries,
                                   Map<String, Long> byType, List<FloorDto> floors) {
            this.totalSlots      = total;
            this.occupied        = occ;
            this.available       = total - occ;
            this.occupancyRate   = total > 0
                ? Math.round(((double) occ / total) * 100.0) : 0;
            this.todayEntries    = todayEntries;
            this.availableByType = byType;
            this.floors          = floors;
        }
    }

    // ── Exit stats ─────────────────────────────────────────────────────────
    public static class ExitStatsResponse {
        public long   totalExits;
        public double revenueCollected;
        public double avgDurationHours;

        public ExitStatsResponse(long exits, double revenue, double avg) {
            this.totalExits        = exits;
            this.revenueCollected  = revenue;
            this.avgDurationHours  = Math.round(avg * 10.0) / 10.0;
        }
    }
}