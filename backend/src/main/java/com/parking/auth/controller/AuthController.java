package com.parking.auth.controller;

import com.parking.auth.dto.AuthResponse;
import com.parking.auth.dto.LoginRequest;
import com.parking.auth.dto.RegisterRequest;
import com.parking.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// NOTE: this class previously carried both Lombok's @RequiredArgsConstructor
// *and* a hand-written constructor with the identical signature — a genuine
// duplicate-constructor compile error that predated this rewrite. Removed
// the annotation and kept the explicit constructor.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req,
                                               HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(req, response));
    }

    // POST /api/auth/refresh  — called automatically by Angular interceptor
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request,
                                                 HttpServletResponse response) {
        return ResponseEntity.ok(authService.refresh(request, response));
    }

    // POST /api/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request,
                                                       HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // POST /api/auth/logout-all  — revoke all sessions for this user
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletResponse response) {
        authService.logoutAll(userDetails.getUsername(), response);
        return ResponseEntity.ok(Map.of("message", "Logged out from all devices"));
    }

    // GET /api/auth/me  — parking user only (UserDetails). Operator JWT uses String principal → 401.
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
                "username", userDetails.getUsername(),
                "roles",    userDetails.getAuthorities().toString()
        ));
    }
}
