package com.parking.auth.scheduler;

import com.parking.auth.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SessionCleanupTask {

    // Defined manually to replace @Slf4j
    private static final Logger log = LoggerFactory.getLogger(SessionCleanupTask.class);

    // NOTE: was previously initialized to `null` here, which made Lombok's
    // @RequiredArgsConstructor skip generating a constructor parameter for
    // it (it only generates one for finals *without* an initializer) — so
    // this bean was never actually injected, and every run of this task
    // threw a NullPointerException (silently caught below). Removing the
    // initializer lets Lombok wire it up for real.
    private final SessionRepository sessionRepository;

    /**
     * Runs every day at midnight (00:00:00)
     * Cron format: second, minute, hour, day of month, month, day(s) of week
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void purgeExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        
        try {
            sessionRepository.deleteAllExpiredBefore(now);
            log.info("Successfully purged expired sessions at {}", now);
        } catch (Exception e) {
            log.error("Failed to purge expired sessions at {}: {}", now, e.getMessage());
        }
    }
}