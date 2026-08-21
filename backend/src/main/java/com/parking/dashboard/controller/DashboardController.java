package com.parking.dashboard.controller;

import com.parking.dashboard.dto.DashboardDto;
import com.parking.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parking")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // GET /api/parking/dashboard/{lotId}
    @GetMapping("/dashboard/{lotId}")
    public ResponseEntity<DashboardDto.DashboardResponse> getDashboard(@PathVariable Long lotId) {
        return ResponseEntity.ok(dashboardService.getDashboard(lotId));
    }

    // GET /api/parking/slots/{slotId} — slot + lot detail for the booking page
    @GetMapping("/slots/{slotId}")
    public ResponseEntity<DashboardDto.SlotDetailDto> getSlot(@PathVariable Long slotId) {
        return ResponseEntity.ok(dashboardService.getSlotDetail(slotId));
    }

    // PATCH /api/parking/slots/{slotId}/toggle — admin manual override only.
    // Regular users book/cancel; operators run entry/exit. This used to be
    // reachable by any authenticated user.
    @PatchMapping("/slots/{slotId}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<DashboardDto.SlotDto> toggleSlot(@PathVariable Long slotId) {
        return ResponseEntity.ok(dashboardService.toggleSlot(slotId));
    }
}
