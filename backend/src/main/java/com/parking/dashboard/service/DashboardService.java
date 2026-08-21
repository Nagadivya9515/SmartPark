package com.parking.dashboard.service;

import com.parking.dashboard.dto.DashboardDto;
import com.parking.inventory.model.ParkingLot;
import com.parking.inventory.model.ParkingSlot;
import com.parking.inventory.model.VehicleType;
import com.parking.inventory.repository.ParkingLotRepository;
import com.parking.inventory.repository.ParkingSlotRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ParkingLotRepository lotRepository;
    private final ParkingSlotRepository slotRepository;

    public DashboardService(ParkingLotRepository lotRepository,
                             ParkingSlotRepository slotRepository) {
        this.lotRepository  = lotRepository;
        this.slotRepository = slotRepository;
    }

    // ── Full dashboard for a lot ───────────────────────────────────────────
    public DashboardDto.DashboardResponse getDashboard(Long lotId) {
        ParkingLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Lot not found: " + lotId));

        List<ParkingSlot> slots = slotRepository.findByLotId(lotId);

        long available = slots.stream().filter(s -> !s.getOccupied()).count();
        long total      = slots.size();

        DashboardDto.SummaryDto summary = new DashboardDto.SummaryDto(
                available, total, lot.getTotalFloors(),
                lot.getName(), lot.getAddress(), lot.getRating());

        // Vehicle type summaries
        List<DashboardDto.VehicleTypeDto> vehicleTypeSummary = new ArrayList<>();
        for (VehicleType type : VehicleType.values()) {
            List<ParkingSlot> ofType = slots.stream()
                    .filter(s -> s.getVehicleType() == type).collect(Collectors.toList());
            long typeAvail = ofType.stream().filter(s -> !s.getOccupied()).count();
            vehicleTypeSummary.add(new DashboardDto.VehicleTypeDto(type.name(), typeAvail, ofType.size()));
        }

        // Sections — derived by grouping slots on (floor, section); there is
        // no separate section row any more, so a synthetic id is assigned
        // per group purely for the frontend to key on.
        Map<String, List<ParkingSlot>> bySectionKey = slots.stream()
                .collect(Collectors.groupingBy(s -> s.getFloorName() + "|" + s.getSectionName(),
                        LinkedHashMap::new, Collectors.toList()));

        List<DashboardDto.SectionDto> sectionDtos = new ArrayList<>();
        long syntheticId = 1;
        for (List<ParkingSlot> group : bySectionKey.values()) {
            ParkingSlot first = group.get(0);
            long secAvail = group.stream().filter(s -> !s.getOccupied()).count();
            List<DashboardDto.SlotDto> slotDtos = group.stream()
                    .map(s -> new DashboardDto.SlotDto(s.getId(), s.getSlotNumber(), s.getOccupied()))
                    .collect(Collectors.toList());
            sectionDtos.add(new DashboardDto.SectionDto(
                    syntheticId++, first.getSectionName(), first.getFloorName(),
                    first.getVehicleType().name(), secAvail, group.size(), slotDtos));
        }

        // Rates — one per vehicle type (first slot of each type)
        List<DashboardDto.RateDto> rates = new ArrayList<>();
        for (VehicleType type : VehicleType.values()) {
            slots.stream()
                    .filter(s -> s.getVehicleType() == type && s.getRatePerHour() != null)
                    .findFirst()
                    .ifPresent(s -> rates.add(new DashboardDto.RateDto(type.name(), s.getRatePerHour())));
        }

        return new DashboardDto.DashboardResponse(summary, vehicleTypeSummary, sectionDtos, rates);
    }

    // ── Single slot detail (for the booking page deep link) ───────────────
    public DashboardDto.SlotDetailDto getSlotDetail(Long slotId) {
        ParkingSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + slotId));
        ParkingLot lot = slot.getLot();
        return new DashboardDto.SlotDetailDto(
                slot.getId(), slot.getSlotNumber(), slot.getDisplayNumber(),
                slot.getSectionName(), slot.getFloorName(), slot.getVehicleType().name(),
                slot.getOccupied(), slot.getRatePerHour(),
                lot.getName(), lot.getAddress());
    }

    // ── Manual slot-occupancy override — administrative only. Regular
    //    users move slot occupancy exclusively through booking/cancellation
    //    and operators through entry/exit; this exists for admin correction
    //    (e.g. clearing a slot stuck occupied). Previously this endpoint had
    //    no role check at all, so any authenticated end user could free or
    //    occupy any slot in the lot — now enforced at the controller with
    //    @PreAuthorize as well as re-checked here. ─────────────────────────
    public DashboardDto.SlotDto toggleSlot(Long slotId) {
        ParkingSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + slotId));
        slot.setOccupied(!slot.getOccupied());
        slotRepository.save(slot);
        return new DashboardDto.SlotDto(slot.getId(), slot.getSlotNumber(), slot.getOccupied());
    }
}
