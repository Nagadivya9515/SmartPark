package com.parking.admin.controller;

import com.parking.admin.dto.AdminDtos;
import com.parking.admin.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // POST /api/admin/auth/login
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody AdminDtos.LoginRequest req) {
        try {
            return ResponseEntity.ok(adminService.login(req));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/admin/overview
    @GetMapping("/overview")
    public ResponseEntity<AdminDtos.OverviewResponse> overview() {
        return ResponseEntity.ok(adminService.overview());
    }

    // GET /api/admin/parking-lots
    @GetMapping("/parking-lots")
    public ResponseEntity<List<AdminDtos.ParkingLotDto>> parkingLots() {
        return ResponseEntity.ok(adminService.getParkingLots());
    }

    // GET /api/admin/operators
    @GetMapping("/operators")
    public ResponseEntity<AdminDtos.OperatorSummaryResponse> operators() {
        return ResponseEntity.ok(adminService.getOperators());
    }

    // POST /api/admin/operators
    @PostMapping("/operators")
    public ResponseEntity<AdminDtos.OperatorDto> createOperator(
            @RequestBody AdminDtos.CreateOperatorRequest req) {
        return ResponseEntity.ok(adminService.createOperator(req));
    }

    // PATCH /api/admin/operators/{id}/toggle
    @PatchMapping("/operators/{id}/toggle")
    public ResponseEntity<AdminDtos.OperatorDto> toggleOperator(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleOperatorStatus(id));
    }

    // DELETE /api/admin/operators/{id}
    @DeleteMapping("/operators/{id}")
    public ResponseEntity<Void> deleteOperator(@PathVariable Long id) {
        adminService.deleteOperator(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/admin/reports?days=7
    @GetMapping("/reports")
    public ResponseEntity<AdminDtos.ReportSummaryResponse> reports(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(adminService.getReports(days));
    }
}