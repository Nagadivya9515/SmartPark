package com.parking.operator.controller;

import com.parking.operator.dto.OperatorDtos;
import com.parking.operator.services.OperatorAuthService;
import com.parking.operator.services.OperatorParkingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/operator")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class OperatorController {

    private final OperatorAuthService    authService;
    private final OperatorParkingService parkingService;

    public OperatorController(OperatorAuthService authService,
                               OperatorParkingService parkingService) {
        this.authService    = authService;
        this.parkingService = parkingService;
    }

    // POST /api/operator/auth/login
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody OperatorDtos.LoginRequest req) {
        try {
            return ResponseEntity.ok(authService.login(req));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/operator/entry
    @PostMapping("/entry")
    public ResponseEntity<?> generateEntry(@RequestBody OperatorDtos.EntryRequest req) {
        try {
            return ResponseEntity.ok(parkingService.generateEntry(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/operator/exit/search?query=TK123456
    @GetMapping("/exit/search")
    public ResponseEntity<?> searchTicket(@RequestParam String query) {
        try {
            return ResponseEntity.ok(parkingService.searchTicket(query));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/operator/exit/complete
    @PostMapping("/exit/complete")
    public ResponseEntity<?> completeExit(@RequestBody OperatorDtos.ExitRequest req) {
        try {
            return ResponseEntity.ok(parkingService.completeExit(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/operator/live-status
    @GetMapping("/live-status")
    public ResponseEntity<OperatorDtos.LiveStatusResponse> liveStatus() {
        return ResponseEntity.ok(parkingService.getLiveStatus());
    }

    // GET /api/operator/exit/stats
    @GetMapping("/exit/stats")
    public ResponseEntity<OperatorDtos.ExitStatsResponse> exitStats() {
        return ResponseEntity.ok(parkingService.getExitStats());
    }
}