package com.parking.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil            = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
    	String token = null;

    	// 1. Try Authorization header first
    	String authHeader = request.getHeader("Authorization");

    	if (authHeader != null && authHeader.startsWith("Bearer ")) {
    	    token = authHeader.substring(7);
    	}

    	// 2. If not found, try cookie
    	if (token == null) {
    	    token = extractCookie(request, "access_token");
    	}
    	
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        String subject = null;
        try {
            subject = jwtUtil.extractUsername(token);
        } catch (Exception e) {
            log.debug("Could not extract username: {}", e.getMessage());
        }

        if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Two token "subjects" exist in this app:
            // - User JWT: subject=username, validated against CustomUserDetailsService
            // - Operator JWT: subject=operatorId, not present in CustomUserDetailsService
            // If we always try userDetailsService, operator endpoints will 401 even with a valid operator token.
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(subject);
                if (jwtUtil.isTokenValid(token, userDetails)) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception userLookupFailed) {
                // Fallback: treat it as an operator JWT (signed with same secret).
                try {
                    String role = jwtUtil.extractClaim(token, claims -> (String) claims.get("role"));
                    var exp = jwtUtil.extractClaim(token, io.jsonwebtoken.Claims::getExpiration);
                    boolean expired = exp != null && exp.before(new java.util.Date());
                    if (!expired) {
                        List<SimpleGrantedAuthority> authorities =
                                role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role));
                        var auth = new UsernamePasswordAuthenticationToken(subject, null, authorities);
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } catch (Exception operatorParseFailed) {
                    // If it's neither a valid user token nor operator token, let it fall through (401 later)
                    log.debug("JWT not valid for user or operator: {}", operatorParseFailed.getMessage());
                }
            }
        }
        chain.doFilter(request, response);
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst().orElse(null);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/login")
            || path.startsWith("/api/auth/register")
            || path.startsWith("/api/auth/refresh");
    }
}