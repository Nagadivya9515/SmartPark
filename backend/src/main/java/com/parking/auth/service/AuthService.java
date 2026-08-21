package com.parking.auth.service;

import com.parking.auth.dto.AuthResponse;
import com.parking.auth.dto.LoginRequest;
import com.parking.auth.dto.RegisterRequest;
import com.parking.auth.model.Session;
import com.parking.auth.model.User;
import com.parking.auth.repository.SessionRepository;
import com.parking.auth.repository.UserRepository;
import com.parking.auth.security.CustomUserDetailsService;
import com.parking.auth.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository           userRepository;
    private final SessionRepository        sessionRepository;
    private final PasswordEncoder          passwordEncoder;
    private final JwtUtil                  jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Value("${jwt.refresh-token-expiry-days}")
    private int refreshExpiryDays;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    public AuthService(UserRepository userRepository,
                       SessionRepository sessionRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       CustomUserDetailsService userDetailsService) {
        this.userRepository     = userRepository;
        this.sessionRepository  = sessionRepository;
        this.passwordEncoder    = passwordEncoder;
        this.jwtUtil            = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new RuntimeException("Username already taken");
        if (userRepository.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email already registered");

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(User.Role.ROLE_USER)
                .build();
        userRepository.save(user);
        return new AuthResponse("Registration successful", user.getUsername(), user.getRole().name());
    }

    @Transactional
    public AuthResponse login(LoginRequest req, HttpServletResponse response) {
        // Fetch user — throw same message for both cases to avoid user enumeration
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        // Validate password with BCrypt directly — no AuthenticationManager needed
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        String accessToken  = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = UUID.randomUUID().toString();

        sessionRepository.save(Session.builder()
                .user(user)
                .refreshToken(refreshToken)
                .expiryDate(LocalDateTime.now().plusDays(refreshExpiryDays))
                .deviceInfo(req.getDeviceInfo())
                .build());

        addCookie(response, "access_token",  accessToken,  3600, "/");
        addCookie(response, "refresh_token", refreshToken, refreshExpiryDays * 86400, "/api/auth/refresh");

        log.info("User '{}' logged in", user.getUsername());
        return new AuthResponse("Login successful", user.getUsername(), user.getRole().name());
    }

    @Transactional
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        // 1. Extract the old refresh token from the secure cookie
        String oldRefreshToken = extractCookie(request, "refresh_token");
        if (oldRefreshToken == null) {
            throw new RuntimeException("Refresh token missing");
        }

        // 2. Find the session and validate
        Session session = sessionRepository.findByRefreshToken(oldRefreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid session"));

        if (session.isExpired()) {
            sessionRepository.delete(session);
            clearCookies(response);
            throw new RuntimeException("Session expired — please login again");
        }

        // 3. Generate NEW tokens (Rotation Strategy)
        UserDetails userDetails = userDetailsService.loadUserByUsername(session.getUser().getUsername());
        String newAccessToken = jwtUtil.generateAccessToken(userDetails);
        String newRefreshToken = UUID.randomUUID().toString(); // New unique refresh identifier

        // 4. Update the Session in DB (Rotate the token)
        session.setRefreshToken(newRefreshToken);
        session.setExpiryDate(LocalDateTime.now().plusDays(7)); // Optional: slide the expiration window
        sessionRepository.save(session);

        // 5. Update Cookies (HttpOnly, Secure, SameSite)
        addCookie(response, "access_token", newAccessToken, 3600, "/"); // 15 mins
        addCookie(response, "refresh_token", newRefreshToken, 604800, "/api/auth/refresh"); // 7 days, restricted path

        return new AuthResponse(
                "Token refreshed",
                session.getUser().getUsername(),
                session.getUser().getRole().name()
        );
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, "refresh_token");
        if (refreshToken != null)
            sessionRepository.deleteByRefreshToken(refreshToken);
        clearCookies(response);
    }

    @Transactional
    public void logoutAll(String username, HttpServletResponse response) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        sessionRepository.deleteByUser(user);
        clearCookies(response);
    }

    private void addCookie(HttpServletResponse res, String name,
                           String value, int maxAge, String path) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);
        res.addCookie(cookie);
    }

    private void clearCookies(HttpServletResponse res) {
        addCookie(res, "access_token",  "", 0, "/");
        addCookie(res, "refresh_token", "", 0, "/api/auth/refresh");
    }

    private String extractCookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return null;
        return Arrays.stream(req.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst().orElse(null);
    }
}