package com.parking.operator.services;

import com.parking.inventory.model.ParkingSlot;
import com.parking.inventory.model.VehicleType;
import com.parking.inventory.repository.ParkingSlotRepository;
import com.parking.operator.dto.OperatorDtos;
import com.parking.operator.models.ParkingTicket;
import com.parking.operator.repository.ParkingTicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OperatorParkingService {

    private static final double SERVICE_FEE = 1.50;

    private final ParkingTicketRepository ticketRepo;
    private final ParkingSlotRepository   slotRepo;

    public OperatorParkingService(ParkingTicketRepository ticketRepo,
                                   ParkingSlotRepository slotRepo) {
        this.ticketRepo = ticketRepo;
        this.slotRepo   = slotRepo;
    }

    // ── Generate entry ticket ──────────────────────────────────────────────
    // @Transactional + a PESSIMISTIC_WRITE query means two entry requests
    // (or an entry racing a booking) for the last slot of a type can no
    // longer both succeed — the second one blocks until the first commits,
    // then correctly finds no slots left. The previous version used a plain
    // unlocked SELECT here.
    @Transactional
    public OperatorDtos.TicketResponse generateEntry(OperatorDtos.EntryRequest req) {
        VehicleType type = VehicleType.valueOf(req.vehicleType);

        List<ParkingSlot> available = slotRepo.findAvailableByVehicleTypeWithLock(type);
        if (available.isEmpty())
            throw new RuntimeException("No slots available for " + req.vehicleType);

        ParkingSlot slot     = available.get(0);
        String      ticketId = generateTicketId();

        slot.setOccupied(true);
        slot.setCurrentTicketId(ticketId);
        slotRepo.save(slot);

        ParkingTicket ticket = new ParkingTicket();
        ticket.setTicketId(ticketId);
        ticket.setVehicleNumber(req.vehicleNumber.toUpperCase().trim());
        ticket.setVehicleType(type);
        ticket.setSlotNumber(slot.getDisplayNumber());
        ticket.setSectionName(slot.getSectionName());
        ticket.setFloorName(slot.getFloorName());
        ticket.setGateName(req.gateName != null ? req.gateName : "Gate 1");
        ticket.setEntryDate(LocalDate.now());
        ticket.setEntryTime(LocalTime.now().withNano(0));
        ticket.setRatePerHour(slot.getRatePerHour());
        ticket.setStatus(ParkingTicket.TicketStatus.ACTIVE);
        ticket.setOperatorId(req.operatorId);
        ticket.setQrData(buildQr(ticketId, slot.getDisplayNumber(), req.vehicleNumber));
        ticketRepo.save(ticket);

        return OperatorDtos.TicketResponse.from(ticket);
    }

    // ── Search ticket (for exit) ───────────────────────────────────────────
    public OperatorDtos.TicketResponse searchTicket(String query) {
        // Try ticketId first, then vehicleNumber
        Optional<ParkingTicket> found = ticketRepo.findByTicketId(query.toUpperCase());
        if (found.isEmpty()) {
            found = ticketRepo.findByVehicleNumberAndStatus(
                    query.toUpperCase(), ParkingTicket.TicketStatus.ACTIVE);
        }
        ParkingTicket ticket = found
                .orElseThrow(() -> new RuntimeException("Ticket not found for: " + query));

        if (ticket.getStatus() != ParkingTicket.TicketStatus.ACTIVE)
            throw new RuntimeException("Ticket is already " + ticket.getStatus().name().toLowerCase());

        // Calculate current duration and amount for preview
        LocalTime now       = LocalTime.now();
        long hours          = Math.max(1, Duration.between(ticket.getEntryTime(), now).toHours() + 1);
        double parkingFee   = ticket.getRatePerHour() * hours;
        double total        = parkingFee + SERVICE_FEE;

        ticket.setDurationHours((int) hours);
        ticket.setParkingFee(parkingFee);
        ticket.setServiceFee(SERVICE_FEE);
        ticket.setTotalAmount(total);
        ticket.setExitTime(now.withNano(0));
        ticket.setExitDate(LocalDate.now());
        // Don't save yet — just preview

        return OperatorDtos.TicketResponse.from(ticket);
    }

    // ── Complete exit ──────────────────────────────────────────────────────
    @Transactional
    public OperatorDtos.TicketResponse completeExit(OperatorDtos.ExitRequest req) {
        ParkingTicket ticket = ticketRepo.findByTicketId(req.ticketId.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (ticket.getStatus() != ParkingTicket.TicketStatus.ACTIVE)
            throw new RuntimeException("Ticket is not active");

        LocalTime exitTime = LocalTime.now().withNano(0);
        long hours         = Math.max(1,
                Duration.between(ticket.getEntryTime(), exitTime).toHours() + 1);
        double parkingFee  = ticket.getRatePerHour() * hours;
        double total       = parkingFee + SERVICE_FEE;

        ticket.setExitDate(LocalDate.now());
        ticket.setExitTime(exitTime);
        ticket.setDurationHours((int) hours);
        ticket.setParkingFee(parkingFee);
        ticket.setServiceFee(SERVICE_FEE);
        ticket.setTotalAmount(total);
        ticket.setPaymentMethod(req.paymentMethod);
        ticket.setStatus(ParkingTicket.TicketStatus.EXITED);
        ticket.setExitOperatorId(req.operatorId);
        ticketRepo.save(ticket);

        // Free slot
        slotRepo.findByCurrentTicketId(req.ticketId).forEach(s -> {
            s.setOccupied(false);
            s.setCurrentTicketId(null);
            slotRepo.save(s);
        });

        return OperatorDtos.TicketResponse.from(ticket);
    }

    // ── Live status ──────────────────────────────────────────────────────────
    public OperatorDtos.LiveStatusResponse getLiveStatus() {
        List<ParkingSlot> all = slotRepo.findAllByOrderByFloorNameAscSectionNameAscSlotNumberAsc();
        long total    = all.size();
        long occupied = all.stream().filter(ParkingSlot::getOccupied).count();
        long today    = ticketRepo.countByEntryDate(LocalDate.now());

        Map<String, Long> byType = new LinkedHashMap<>();
        byType.put("CAR",   slotRepo.countAvailableByType(VehicleType.CAR));
        byType.put("BIKE",  slotRepo.countAvailableByType(VehicleType.BIKE));
        byType.put("TRUCK", slotRepo.countAvailableByType(VehicleType.TRUCK));

        Map<String, List<ParkingSlot>> byFloor = all.stream()
                .collect(Collectors.groupingBy(ParkingSlot::getFloorName,
                         LinkedHashMap::new, Collectors.toList()));

        List<OperatorDtos.FloorDto> floors = byFloor.entrySet().stream().map(fe -> {
            long floorAvail = fe.getValue().stream().filter(s -> !s.getOccupied()).count();

            Map<String, List<ParkingSlot>> bySection = fe.getValue().stream()
                    .collect(Collectors.groupingBy(ParkingSlot::getSectionName,
                             LinkedHashMap::new, Collectors.toList()));

            List<OperatorDtos.SectionDto> sections = bySection.entrySet().stream().map(se -> {
                List<ParkingSlot> ss     = se.getValue();
                long secAvail            = ss.stream().filter(s -> !s.getOccupied()).count();
                List<OperatorDtos.SlotDto> slotDtos =
                        ss.stream().map(OperatorDtos.SlotDto::from).collect(Collectors.toList());
                return new OperatorDtos.SectionDto(se.getKey(),
                        ss.get(0).getVehicleType().name(), secAvail,
                        ss.size() - secAvail, slotDtos);
            }).collect(Collectors.toList());

            return new OperatorDtos.FloorDto(fe.getKey(), fe.getValue().size(), floorAvail, sections);
        }).collect(Collectors.toList());

        return new OperatorDtos.LiveStatusResponse(total, occupied, today, byType, floors);
    }

    // ── Exit stats ────────────────────────────────────────────────────────────
    public OperatorDtos.ExitStatsResponse getExitStats() {
        LocalDate today = LocalDate.now();
        long   exits    = ticketRepo.countExitsByDate(today);
        double revenue  = ticketRepo.sumRevenueByDate(today);
        double avgDur   = ticketRepo.avgDurationByDate(today);
        return new OperatorDtos.ExitStatsResponse(exits, revenue, avgDur);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private String generateTicketId() {
        return "TK" + (100000 + new Random().nextInt(900000));
    }

    private String buildQr(String ticketId, String slot, String vehicle) {
        return String.format("{\"t\":\"%s\",\"s\":\"%s\",\"v\":\"%s\",\"d\":\"%s\"}",
                ticketId, slot, vehicle, LocalDate.now());
    }
}
