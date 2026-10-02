package com.parking.auth;

import com.parking.IntegrationTestBase;
import com.parking.TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the session cleanup scheduled job.
 * Ensures expired sessions are cleaned up reliably without NPE.
 * 
 * CRITICAL SCENARIOS:
 * 1. Job runs without NullPointerException (verifies NPE fix)
 * 2. Expired sessions are removed from database
 * 3. Active sessions are preserved
 * 4. Job handles empty session table gracefully
 * 5. Mixed expired and active sessions handled correctly
 * 6. Sessions at expiry boundary cleaned up correctly
 * 7. Performance acceptable for large datasets
 */
@DisplayName("Session Cleanup Job Integration Tests")
public class SessionCleanupJobIT extends IntegrationTestBase {

    @Autowired
    private SessionCleanupJob sessionCleanupJob;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    @Transactional
    void setUp() {
        userSessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should run cleanup job without NullPointerException")
    void testCleanupJobRunsWithoutNPE() {
        // This test verifies the fix for the NPE in SessionCleanupJob
        // where repository field was initialized to null
        
        assertThat(() -> sessionCleanupJob.cleanupExpiredSessions())
            .as("SessionCleanupJob should not throw NPE")
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should remove expired sessions")
    void testExpiredSessionsAreRemoved() {
        // Create test user
        User user = createTestUser(TestSupport.TestData.USER_EMAIL);

        // Create an expired session (8 days old, default expiry is 7 days)
        UserSession expiredSession = new UserSession();
        expiredSession.setUser(user);
        expiredSession.setTokenHash("hash_expired_" + System.currentTimeMillis());
        expiredSession.setCreatedAt(LocalDateTime.now().minusDays(8));
        expiredSession.setExpiresAt(LocalDateTime.now().minusDays(1));  // Expired yesterday
        UserSession saved = userSessionRepository.save(expiredSession);
        long sessionId = saved.getId();

        assertThat(userSessionRepository.findById(sessionId))
            .as("Session should exist before cleanup")
            .isPresent();

        // Run cleanup job
        sessionCleanupJob.cleanupExpiredSessions();

        // Verify session is removed
        assertThat(userSessionRepository.findById(sessionId))
            .as("Expired session should be deleted by cleanup job")
            .isEmpty();
    }

    @Test
    @DisplayName("Should preserve active sessions")
    void testActiveSessionsPreserved() {
        User user = createTestUser(TestSupport.TestData.USER_EMAIL);

        // Create an active session (expires tomorrow)
        UserSession activeSession = new UserSession();
        activeSession.setUser(user);
        activeSession.setTokenHash("hash_active_" + System.currentTimeMillis());
        activeSession.setCreatedAt(LocalDateTime.now());
        activeSession.setExpiresAt(LocalDateTime.now().plusDays(1));  // Expires tomorrow
        UserSession saved = userSessionRepository.save(activeSession);
        long sessionId = saved.getId();

        // Run cleanup
        sessionCleanupJob.cleanupExpiredSessions();

        // Verify session still exists
        assertThat(userSessionRepository.findById(sessionId))
            .as("Active session should be preserved by cleanup job")
            .isPresent();
    }

    @Test
    @DisplayName("Should handle mixed expired and active sessions")
    void testCleanupWithMixedSessions() {
        User user = createTestUser(TestSupport.TestData.USER_EMAIL);

        // Create 3 expired sessions
        for (int i = 0; i < 3; i++) {
            UserSession expiredSession = new UserSession();
            expiredSession.setUser(user);
            expiredSession.setTokenHash("hash_expired_" + i + "_" + System.currentTimeMillis());
            expiredSession.setCreatedAt(LocalDateTime.now().minusDays(10 + i));
            expiredSession.setExpiresAt(LocalDateTime.now().minusDays(3 + i));
            userSessionRepository.save(expiredSession);
        }

        // Create 2 active sessions
        long activeSession1Id = saveActiveSession(user, LocalDateTime.now().plusDays(1)).getId();
        long activeSession2Id = saveActiveSession(user, LocalDateTime.now().plusDays(7)).getId();

        long totalBefore = userSessionRepository.count();
        assertThat(totalBefore).isEqualTo(5);

        // Run cleanup
        sessionCleanupJob.cleanupExpiredSessions();

        // Verify exactly 2 active sessions remain
        long totalAfter = userSessionRepository.count();
        assertThat(totalAfter)
            .as("Should have exactly 2 active sessions after cleanup")
            .isEqualTo(2);

        assertThat(userSessionRepository.findById(activeSession1Id))
            .as("First active session should be preserved")
            .isPresent();

        assertThat(userSessionRepository.findById(activeSession2Id))
            .as("Second active session should be preserved")
            .isPresent();
    }

    @Test
    @DisplayName("Should handle cleanup on empty session table")
    void testCleanupOnEmptyTable() {
        long initialCount = userSessionRepository.count();
        assertThat(initialCount).isZero();

        // Should not throw any exception
        assertThat(() -> sessionCleanupJob.cleanupExpiredSessions())
            .doesNotThrowAnyException();

        assertThat(userSessionRepository.count())
            .as("Table should remain empty")
            .isEqualTo(0);
    }

    @Test
    @DisplayName("Should clean up sessions exactly at expiry boundary")
    void testCleanupAtExpiryBoundary() {
        User user = createTestUser(TestSupport.TestData.USER_EMAIL);

        // Create session with expiry exactly at current time
        LocalDateTime expiryTime = LocalDateTime.now();
        UserSession boundarySession = new UserSession();
        boundarySession.setUser(user);
        boundarySession.setTokenHash("hash_boundary_" + System.currentTimeMillis());
        boundarySession.setCreatedAt(LocalDateTime.now().minusDays(1));
        boundarySession.setExpiresAt(expiryTime);
        UserSession saved = userSessionRepository.save(boundarySession);
        long sessionId = saved.getId();

        assertThat(userSessionRepository.findById(sessionId))
            .as("Session should exist before cleanup")
            .isPresent();

        // Run cleanup
        sessionCleanupJob.cleanupExpiredSessions();

        // Session at or before current time should be removed
        assertThat(userSessionRepository.findById(sessionId))
            .as("Session at exact expiry boundary should be removed")
            .isEmpty();
    }

    @Test
    @DisplayName("Should complete cleanup within reasonable time for 100 sessions")
    void testCleanupPerformance() {
        User user = createTestUser(TestSupport.TestData.USER_EMAIL);

        // Create 100 expired sessions
        for (int i = 0; i < 100; i++) {
            UserSession expiredSession = new UserSession();
            expiredSession.setUser(user);
            expiredSession.setTokenHash("hash_perf_" + i + "_" + System.currentTimeMillis());
            expiredSession.setCreatedAt(LocalDateTime.now().minusDays(10));
            expiredSession.setExpiresAt(LocalDateTime.now().minusDays(2));
            userSessionRepository.save(expiredSession);
        }

        long countBefore = userSessionRepository.count();
        assertThat(countBefore).isEqualTo(100);

        // Measure cleanup time
        long startTime = System.currentTimeMillis();
        sessionCleanupJob.cleanupExpiredSessions();
        long duration = System.currentTimeMillis() - startTime;

        assertThat(duration)
            .as("Cleanup should complete within 5 seconds for 100 sessions")
            .isLessThan(5000);

        assertThat(userSessionRepository.count())
            .as("All expired sessions should be removed")
            .isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle multiple sessions per user")
    void testMultipleSessionsPerUser() {
        User user = createTestUser(TestSupport.TestData.USER_EMAIL);

        // Create 5 expired sessions for same user
        for (int i = 0; i < 5; i++) {
            UserSession session = new UserSession();
            session.setUser(user);
            session.setTokenHash("hash_multi_" + i + "_" + System.currentTimeMillis());
            session.setCreatedAt(LocalDateTime.now().minusDays(10));
            session.setExpiresAt(LocalDateTime.now().minusDays(1));
            userSessionRepository.save(session);
        }

        // Create 2 active sessions for same user
        long activeSession1Id = saveActiveSession(user, LocalDateTime.now().plusDays(1)).getId();
        long activeSession2Id = saveActiveSession(user, LocalDateTime.now().plusDays(3)).getId();

        assertThat(userSessionRepository.count()).isEqualTo(7);

        // Run cleanup
        sessionCleanupJob.cleanupExpiredSessions();

        // Verify only active sessions remain
        assertThat(userSessionRepository.count()).isEqualTo(2);
        assertThat(userSessionRepository.findById(activeSession1Id)).isPresent();
        assertThat(userSessionRepository.findById(activeSession2Id)).isPresent();
    }

    // ─── Helper Methods ───

    @Transactional
    private User createTestUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(TestSupport.TestData.USER_PASSWORD));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(User.Role.USER);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    @Transactional
    private UserSession saveActiveSession(User user, LocalDateTime expiresAt) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setTokenHash("hash_active_" + System.currentTimeMillis());
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(expiresAt);
        return userSessionRepository.save(session);
    }
}
