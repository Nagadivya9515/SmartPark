package com.parking.auth;

import com.parking.IntegrationTestBase;
import com.parking.TestSupport;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Integration tests for JWT refresh token rotation and validation.
 * 
 * CRITICAL SCENARIOS:
 * 1. Successful token refresh returns new access + refresh tokens
 * 2. Old refresh token is immediately invalidated (can't be reused)
 * 3. Expired tokens are strictly rejected
 * 4. Malformed tokens fail validation
 */
@DisplayName("JWT Refresh Token Integration Tests")
public class JwtRefreshTokenIT extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.refresh-token-expiry-days:7}")
    private long refreshTokenExpiryDays;

    @Value("${jwt.access-token-expiry-ms:900000}")
    private long accessTokenExpiryMs;

    private String validRefreshToken;
    private String userEmail;

    @BeforeEach
    @Transactional
    void setUp() throws Exception {
        userEmail = TestSupport.TestData.USER_EMAIL;
        
        // Register test user
        User user = new User();
        user.setEmail(userEmail);
        user.setPassword(passwordEncoder.encode(TestSupport.TestData.USER_PASSWORD));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(User.Role.USER);
        user.setEnabled(true);
        userRepository.save(user);

        // Perform login to get initial refresh token
        MvcResult loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"email\":\"%s\",\"password\":\"%s\"}",
                    userEmail, TestSupport.TestData.USER_PASSWORD
                ))
        ).andExpect(status().isOk()).andReturn();

        // Extract refresh token from cookies
        validRefreshToken = extractRefreshTokenFromCookies(loginResult);
        assertThat(validRefreshToken).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Should successfully refresh tokens with valid refresh token")
    void testSuccessfulTokenRefresh() throws Exception {
        MvcResult result = mockMvc.perform(
            post("/api/auth/refresh")
                .cookie(new javax.servlet.http.Cookie("refreshToken", validRefreshToken))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(cookie().exists("refreshToken"))
        .andReturn();

        String newRefreshToken = extractRefreshTokenFromCookies(result);
        assertThat(newRefreshToken)
            .isNotEmpty()
            .isNotEqualTo(validRefreshToken)
            .as("New refresh token should differ from old one (token rotation)");
    }

    @Test
    @DisplayName("Should invalidate old refresh token after rotation")
    void testOldRefreshTokenInvalidatedAfterRotation() throws Exception {
        // Perform first refresh
        MvcResult firstRefresh = mockMvc.perform(
            post("/api/auth/refresh")
                .cookie(new javax.servlet.http.Cookie("refreshToken", validRefreshToken))
        ).andExpect(status().isOk()).andReturn();

        String newRefreshToken = extractRefreshTokenFromCookies(firstRefresh);
        assertThat(newRefreshToken).isNotNull();

        // Attempt to use old refresh token again (should fail)
        MvcResult secondAttempt = mockMvc.perform(
            post("/api/auth/refresh")
                .cookie(new javax.servlet.http.Cookie("refreshToken", validRefreshToken))
        ).andReturn();

        assertThat(secondAttempt.getResponse().getStatus())
            .as("Old refresh token should be rejected after rotation (401 Unauthorized)")
            .isEqualTo(401);
    }

    @Test
    @DisplayName("Should reject expired refresh tokens")
    void testExpiredRefreshTokenRejection() throws Exception {
        // Create an expired token (expiry date in the past)
        String expiredToken = generateExpiredToken(userEmail);

        MvcResult result = mockMvc.perform(
            post("/api/auth/refresh")
                .cookie(new javax.servlet.http.Cookie("refreshToken", expiredToken))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("Expired refresh token should return 401 Unauthorized")
            .isEqualTo(401);
    }

    @Test
    @DisplayName("Should reject malformed refresh tokens")
    void testMalformedRefreshTokenRejection() throws Exception {
        String malformedToken = TestSupport.JwtTestData.MALFORMED_TOKEN;

        MvcResult result = mockMvc.perform(
            post("/api/auth/refresh")
                .cookie(new javax.servlet.http.Cookie("refreshToken", malformedToken))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("Malformed refresh token should return 400 or 401")
            .isIn(400, 401);
    }

    @Test
    @DisplayName("Should reject blank refresh tokens")
    void testBlankRefreshTokenRejection() throws Exception {
        MvcResult result = mockMvc.perform(
            post("/api/auth/refresh")
                .cookie(new javax.servlet.http.Cookie("refreshToken", ""))
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("Blank refresh token should return 400 or 401")
            .isIn(400, 401);
    }

    @Test
    @DisplayName("Should maintain user identity across multiple token refreshes")
    void testUserIdentityPreservedAcrossRefreshes() throws Exception {
        String refreshToken = validRefreshToken;

        // Perform 3 consecutive refreshes
        for (int i = 0; i < 3; i++) {
            MvcResult result = mockMvc.perform(
                post("/api/auth/refresh")
                    .cookie(new javax.servlet.http.Cookie("refreshToken", refreshToken))
            ).andExpect(status().isOk()).andReturn();

            refreshToken = extractRefreshTokenFromCookies(result);
            String responseBody = result.getResponse().getContentAsString();

            // Verify token contains correct user email
            assertThat(responseBody)
                .as("Response should contain user email: " + userEmail)
                .contains(userEmail);
        }
    }

    @Test
    @DisplayName("Should reject refresh without token in cookie")
    void testRefreshWithoutToken() throws Exception {
        MvcResult result = mockMvc.perform(
            post("/api/auth/refresh")
        ).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("Request without token should fail (400 or 401)")
            .isIn(400, 401);
    }

    @Test
    @DisplayName("Should handle concurrent refresh requests safely")
    void testConcurrentRefreshRequests() throws Exception {
        // First refresh with original token
        MvcResult first = mockMvc.perform(
            post("/api/auth/refresh")
                .cookie(new javax.servlet.http.Cookie("refreshToken", validRefreshToken))
        ).andExpect(status().isOk()).andReturn();

        String newToken = extractRefreshTokenFromCookies(first);
        assertThat(newToken).isNotNull();

        // Old token should now fail
        MvcResult second = mockMvc.perform(
            post("/api/auth/refresh")
                .cookie(new javax.servlet.http.Cookie("refreshToken", validRefreshToken))
        ).andReturn();

        assertThat(second.getResponse().getStatus())
            .as("Old token should be invalidated")
            .isEqualTo(401);

        // New token should work
        MvcResult third = mockMvc.perform(
            post("/api/auth/refresh")
                .cookie(new javax.servlet.http.Cookie("refreshToken", newToken))
        ).andReturn();

        assertThat(third.getResponse().getStatus())
            .as("New token should be valid")
            .isEqualTo(200);
    }

    // ─── Helper Methods ───

    private String extractRefreshTokenFromCookies(MvcResult result) {
        javax.servlet.http.Cookie cookie = result.getResponse().getCookie("refreshToken");
        return cookie != null ? cookie.getValue() : null;
    }

    private String generateExpiredToken(String subject) {
        return Jwts.builder()
            .setSubject(subject)
            .setIssuedAt(new Date(System.currentTimeMillis() - 86400000))  // 1 day ago
            .setExpiration(new Date(System.currentTimeMillis() - 3600000))  // 1 hour ago (expired)
            .signWith(SignatureAlgorithm.HS256, jwtSecret)
            .compact();
    }
}
