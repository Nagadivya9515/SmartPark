package com.parking.operator.services;

import com.parking.operator.dto.OperatorDtos;
import com.parking.operator.models.Operator;
import com.parking.operator.repository.OperatorRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;

@Service
public class OperatorAuthService {

    private final OperatorRepository operatorRepository;
    private final PasswordEncoder    passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.operator-expiry-ms:28800000}")
    private long jwtExpiryMs;   // 8 hours default

    public OperatorAuthService(OperatorRepository operatorRepository,
                                PasswordEncoder passwordEncoder) {
        this.operatorRepository = operatorRepository;
        this.passwordEncoder    = passwordEncoder;
    }

    public OperatorDtos.LoginResponse login(OperatorDtos.LoginRequest req) {
        Operator op = operatorRepository.findByOperatorId(req.operatorId)
                .orElseThrow(() -> new RuntimeException("Invalid Operator ID or password"));

        if (!op.getActive())
            throw new RuntimeException("Operator account is inactive");

        if (!passwordEncoder.matches(req.password, op.getPassword()))
            throw new RuntimeException("Invalid Operator ID or password");

        // Update last login
        op.setLastLogin(LocalDateTime.now());
        operatorRepository.save(op);

        String token = generateToken(op);
        return new OperatorDtos.LoginResponse(token, op);
    }

    private String generateToken(Operator op) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.builder()
                .setSubject(op.getOperatorId())
                .claim("role",     op.getRole().name())
                .claim("gateName", op.getGateName())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiryMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}