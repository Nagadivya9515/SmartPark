package com.parking.admin.dto;

import com.parking.operator.models.Operator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AdminDtos {

    // ── Auth ───────────────────────────────────────────────────────────────
    public static class LoginRequest {
        public String username;
        public String password;
        public boolean rememberMe;
    }

    public static class LoginResponse {
        public String token;
        public String username;
        public String fullName;
        public String role;
        public String message;

        public LoginResponse(String token, String username, String fullName, String role) {
            this.token    = token;
            this.username = username;
            this.fullName = fullName;
            this.role     = role;
            this.message  = "Login successful";
        }
    }

    // ── Dashboard overview ─────────────────────────────────────────────────
    public static class DailyPoint {
        public String date;
        public double value;
        public DailyPoint(LocalDate date, double value) {
            this.date  = date.toString();
            this.value = value;
        }
    }

    public static class OverviewResponse {
        public double            todayRevenue;
        public double            yesterdayRevenue;
        public long              activeVehicles;
        public long              totalSlots;
        public long              availableSlots;
        public long              activeOperators;
        public long              totalOperators;
        public double            occupancyRate;
        public List<DailyPoint>  revenueTrend7d;
        public List<DailyPoint>  trafficTrend7d;
        public Map<String,Long>  occupancyByType;

        public OverviewResponse(double todayRevenue, double yesterdayRevenue,
                                 long activeVehicles, long totalSlots,
                                 long availableSlots, long activeOperators, long totalOperators,
                                 List<DailyPoint> revenueTrend7d,
                                 List<DailyPoint> trafficTrend7d,
                                 Map<String,Long> occupancyByType) {
            this.todayRevenue     = todayRevenue;
            this.yesterdayRevenue = yesterdayRevenue;
            this.activeVehicles   = activeVehicles;
            this.totalSlots       = totalSlots;
            this.availableSlots   = availableSlots;
            this.activeOperators  = activeOperators;
            this.totalOperators   = totalOperators;
            this.occupancyRate    = totalSlots > 0
                ? Math.round(((double)(totalSlots - availableSlots) / totalSlots) * 100.0) : 0;
            this.revenueTrend7d   = revenueTrend7d;
            this.trafficTrend7d   = trafficTrend7d;
            this.occupancyByType  = occupancyByType;
        }
    }

    // ── Parking Lot ────────────────────────────────────────────────────────
    public static class ParkingLotDto {
        public Long    id;
        public String  name;
        public String  address;
        public long    totalSlots;
        public long    availableSlots;
        public double  rating;
        public boolean active;
        public double  carRate;
        public double  bikeRate;
        public double  truckRate;

        public ParkingLotDto(Long id, String name, String address,
                              long total, long avail, double rating,
                              boolean active, double carRate, double bikeRate, double truckRate) {
            this.id            = id;
            this.name          = name;
            this.address       = address;
            this.totalSlots    = total;
            this.availableSlots= avail;
            this.rating        = rating;
            this.active        = active;
            this.carRate       = carRate;
            this.bikeRate      = bikeRate;
            this.truckRate     = truckRate;
        }
    }

    // ── Operator ───────────────────────────────────────────────────────────
    public static class OperatorDto {
        public Long    id;
        public String  operatorId;
        public String  fullName;
        public String  email;
        public String  phone;
        public String  role;
        public String  gateName;
        public String  assignedLot;
        public boolean active;
        public String  initials;

        public static OperatorDto from(Operator op) {
            OperatorDto d   = new OperatorDto();
            d.id            = op.getId();
            d.operatorId    = op.getOperatorId();
            d.fullName      = op.getFullName();
            d.email         = op.getOperatorId().toLowerCase().replace(" ", ".") + "@parking.com";
            d.phone         = "+1-555-010" + (op.getId() % 10);
            d.role          = formatRole(op.getRole().name());
            d.gateName      = op.getGateName();
            d.assignedLot   = op.getLotName();
            d.active        = op.getActive();
            d.initials      = initials(op.getFullName());
            return d;
        }

        private static String formatRole(String role) {
            return switch (role) {
                case "ENTRY_OPERATOR" -> "Entry";
                case "EXIT_OPERATOR"  -> "Exit";
                case "SUPERVISOR"     -> "Entry & Exit";
                default               -> role;
            };
        }

        private static String initials(String name) {
            if (name == null || name.isBlank()) return "?";
            String[] parts = name.trim().split("\\s+");
            if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
    }

    public static class OperatorSummaryResponse {
        public long              total;
        public long              active;
        public long              inactive;
        public long              entryAndExit;
        public List<OperatorDto> operators;

        public OperatorSummaryResponse(long total, long active,
                                        long inactive, long entryAndExit,
                                        List<OperatorDto> operators) {
            this.total        = total;
            this.active       = active;
            this.inactive     = inactive;
            this.entryAndExit = entryAndExit;
            this.operators    = operators;
        }
    }

    public static class CreateOperatorRequest {
        public String operatorId;
        public String fullName;
        public String password;
        public String role;
        public String gateName;
        public String lotName;
    }

    // ── Reports ────────────────────────────────────────────────────────────
    public static class ReportSummaryResponse {
        public double            totalRevenue;
        public long              totalVehicles;
        public double            avgRevenuePerDay;
        public double            avgVehiclesPerDay;
        public double            revenueTrend;     // % change vs prev period
        public double            trafficTrend;
        public List<DailyPoint>  revenueTrend7d;
        public List<DailyPoint>  trafficTrend7d;
        public Map<String,Double> revenueByPayment;
        public Map<String,Double> revenueByVehicle;

        public ReportSummaryResponse(double totalRevenue, long totalVehicles,
                                      double revTrend, double trfTrend,
                                      List<DailyPoint> revTrend7d,
                                      List<DailyPoint> trfTrend7d,
                                      Map<String,Double> byPayment,
                                      Map<String,Double> byVehicle) {
            this.totalRevenue      = totalRevenue;
            this.totalVehicles     = totalVehicles;
            this.avgRevenuePerDay  = totalVehicles > 0 ? Math.round(totalRevenue / 7.0 * 100.0) / 100.0 : 0;
            this.avgVehiclesPerDay = Math.round((double) totalVehicles / 7.0 * 10.0) / 10.0;
            this.revenueTrend      = revTrend;
            this.trafficTrend      = trfTrend;
            this.revenueTrend7d    = revTrend7d;
            this.trafficTrend7d    = trfTrend7d;
            this.revenueByPayment  = byPayment;
            this.revenueByVehicle  = byVehicle;
        }
    }
}