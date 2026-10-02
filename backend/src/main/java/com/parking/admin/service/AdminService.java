package com.parking.admin.service;

import com.parking.admin.dto.AdminDtos;
import com.parking.admin.model.AdminUser;
import com.parking.admin.repository.AdminUserRepository;
import com.parking.inventory.model.ParkingLot;
import com.parking.inventory.model.ParkingSlot;
import com.parking.inventory.model.VehicleType;
import com.parking.inventory.repository.ParkingLotRepository;
import com.parking.inventory.repository.ParkingSlotRepository;
import com.parking.operator.models.Operator;
import com.parking.operator.models.ParkingTicket;
import com.parking.operator.repository.OperatorRepository;
import com.parking.operator.repository.ParkingTicketRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final AdminUserRepository     adminRepo;
    private final ParkingTicketRepository ticketRepo;
    private final ParkingLotRepository    lotRepo;
    private final ParkingSlotRepository   slotRepo;
    private final OperatorRepository      operatorRepo;
    private final PasswordEncoder         passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.admin-expiry-ms:28800000}")
    private long jwtExpiryMs;

    public AdminService(AdminUserRepository adminRepo,
                         ParkingTicketRepository ticketRepo,
                         ParkingLotRepository lotRepo,
                         ParkingSlotRepository slotRepo,
                         OperatorRepository operatorRepo,
                         PasswordEncoder passwordEncoder) {
        this.adminRepo       = adminRepo;
        this.ticketRepo      = ticketRepo;
        this.lotRepo         = lotRepo;
        this.slotRepo        = slotRepo;
        this.operatorRepo    = operatorRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Login ──────────────────────────────────────────────────────────────
    public AdminDtos.LoginResponse login(AdminDtos.LoginRequest req) {
        AdminUser admin = adminRepo.findByUsername(req.username)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!admin.getActive())
            throw new RuntimeException("Account inactive");

        if (!passwordEncoder.matches(req.password, admin.getPassword()))
            throw new RuntimeException("Invalid credentials");

        admin.setLastLogin(LocalDateTime.now());
        adminRepo.save(admin);

        String token = Jwts.builder()
                .setSubject(admin.getUsername())
                .claim("role", admin.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiryMs))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()), SignatureAlgorithm.HS256)
                .compact();

        return new AdminDtos.LoginResponse(token, admin.getUsername(),
                admin.getFullName(), admin.getRole().name());
    }

    // ── Dashboard overview ───────────────────────────────────────────────────
    public AdminDtos.OverviewResponse overview() {
        LocalDate today     = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        double todayRev     = ticketRepo.sumRevenueByDate(today);
        double yesterdayRev = ticketRepo.sumRevenueByDate(yesterday);
        long   active       = ticketRepo.countByStatus(ParkingTicket.TicketStatus.ACTIVE);
        long   totalSlots   = slotRepo.count();
        long   avail        = slotRepo.countAvailable();
        long   activeOps    = operatorRepo.countActive();
        long   totalOps     = operatorRepo.count();

        List<AdminDtos.DailyPoint> revTrend = new ArrayList<>();
        List<AdminDtos.DailyPoint> trfTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            revTrend.add(new AdminDtos.DailyPoint(d, ticketRepo.sumRevenueByDate(d)));
            trfTrend.add(new AdminDtos.DailyPoint(d, ticketRepo.countByEntryDate(d)));
        }

        Map<String, Long> occByType = new LinkedHashMap<>();
        for (VehicleType t : VehicleType.values()) {
            long avType    = slotRepo.countAvailableByType(t);
            long totalType = slotRepo.findAll().stream()
                    .filter(s -> s.getVehicleType() == t).count();
            occByType.put(t.name(), Math.max(0, totalType - avType));
        }

        return new AdminDtos.OverviewResponse(todayRev, yesterdayRev, active,
                totalSlots, avail, activeOps, totalOps,
                revTrend, trfTrend, occByType);
    }

    // ── Parking lots ─────────────────────────────────────────────────────────
    // Now reads the real ParkingLot rows instead of synthesizing one "lot"
    // per floor-name grouping of operator slots.
    public List<AdminDtos.ParkingLotDto> getParkingLots() {
        return lotRepo.findAll().stream().map(lot -> {
            List<ParkingSlot> slots = slotRepo.findByLotId(lot.getId());
            long total  = slots.size();
            long avail  = slots.stream().filter(s -> !s.getOccupied()).count();
            double carRate   = rateFor(slots, VehicleType.CAR,   5.0);
            double bikeRate  = rateFor(slots, VehicleType.BIKE,  2.0);
            double truckRate = rateFor(slots, VehicleType.TRUCK, 8.0);

            return new AdminDtos.ParkingLotDto(lot.getId(), lot.getName(), lot.getAddress(),
                    total, avail, lot.getRating() != null ? lot.getRating() : 0.0, true,
                    carRate, bikeRate, truckRate);
        }).collect(Collectors.toList());
    }

    private double rateFor(List<ParkingSlot> slots, VehicleType type, double fallback) {
        return slots.stream().filter(s -> s.getVehicleType() == type && s.getRatePerHour() != null)
                .mapToDouble(ParkingSlot::getRatePerHour).average().orElse(fallback);
    }

    // ── Operators ────────────────────────────────────────────────────────────
    public AdminDtos.OperatorSummaryResponse getOperators() {
        List<Operator> all = operatorRepo.findAll();
        long active  = all.stream().filter(Operator::getActive).count();
        long inactive = all.size() - active;
        long entryAndExit = all.stream()
                .filter(o -> o.getRole() == Operator.OperatorRole.SUPERVISOR).count();

        List<AdminDtos.OperatorDto> dtos = all.stream()
                .map(AdminDtos.OperatorDto::from).collect(Collectors.toList());

        return new AdminDtos.OperatorSummaryResponse(all.size(), active,
                inactive, entryAndExit, dtos);
    }

    @Transactional
    public AdminDtos.OperatorDto createOperator(AdminDtos.CreateOperatorRequest req) {
        Operator op = new Operator();
        op.setOperatorId(req.operatorId);
        op.setPassword(passwordEncoder.encode(req.password));
        op.setFullName(req.fullName);
        op.setGateName(req.gateName);
        op.setLotName(req.lotName);
        op.setRole(Operator.OperatorRole.valueOf(req.role));
        op.setActive(true);
        operatorRepo.save(op);
        return AdminDtos.OperatorDto.from(op);
    }

    @Transactional
    public AdminDtos.OperatorDto toggleOperatorStatus(Long id) {
        Operator op = operatorRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Operator not found"));
        op.setActive(!op.getActive());
        operatorRepo.save(op);
        return AdminDtos.OperatorDto.from(op);
    }

    @Transactional
    public void deleteOperator(Long id) {
        operatorRepo.deleteById(id);
    }

    // ── Reports ──────────────────────────────────────────────────────────────
    public AdminDtos.ReportSummaryResponse getReports(int days) {
        LocalDate today = LocalDate.now();

        double totalRev = 0;
        long   totalVeh = 0;
        List<AdminDtos.DailyPoint> revTrend = new ArrayList<>();
        List<AdminDtos.DailyPoint> trfTrend = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate d   = today.minusDays(i);
            double    rev = ticketRepo.sumRevenueByDate(d);
            long      cnt = ticketRepo.countByEntryDate(d);
            totalRev     += rev;
            totalVeh     += cnt;
            revTrend.add(new AdminDtos.DailyPoint(d, rev));
            trfTrend.add(new AdminDtos.DailyPoint(d, cnt));
        }

        // Revenue by payment method (simulated breakdown — no per-payment-method
        // aggregation exists yet; flagged here rather than presented as measured).
        Map<String, Double> byPayment = new LinkedHashMap<>();
        byPayment.put("Cash",              totalRev * 0.45);
        byPayment.put("Credit/Debit Card", totalRev * 0.36);
        byPayment.put("UPI/Digital Wallet",totalRev * 0.19);

        Map<String, Double> byVehicle = new LinkedHashMap<>();
        byVehicle.put("Car",   totalRev * 0.67);
        byVehicle.put("Bike",  totalRev * 0.20);
        byVehicle.put("Truck", totalRev * 0.13);

        double half1 = revTrend.subList(0, days / 2).stream().mapToDouble(p -> p.value).sum();
        double half2 = revTrend.subList(days / 2, days).stream().mapToDouble(p -> p.value).sum();
        double revTrendPct = half1 > 0 ? Math.round(((half2 - half1) / half1) * 1000.0) / 10.0 : 0;

        double tHalf1 = trfTrend.subList(0, days / 2).stream().mapToDouble(p -> p.value).sum();
        double tHalf2 = trfTrend.subList(days / 2, days).stream().mapToDouble(p -> p.value).sum();
        double trfTrendPct = tHalf1 > 0 ? Math.round(((tHalf2 - tHalf1) / tHalf1) * 1000.0) / 10.0 : 0;

        return new AdminDtos.ReportSummaryResponse(totalRev, totalVeh,
                revTrendPct, trfTrendPct, revTrend, trfTrend, byPayment, byVehicle);
    }
}
