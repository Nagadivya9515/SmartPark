package com.parking.auth.repository;

import com.parking.auth.model.Session;
import com.parking.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByRefreshToken(String refreshToken);

    @Modifying
    @Transactional
    void deleteByRefreshToken(String refreshToken);

    @Modifying
    @Transactional
    void deleteByUser(User user);

    // Scheduled cleanup: remove all expired sessions
    @Modifying
    @Transactional
    @Query("DELETE FROM Session s WHERE s.expiryDate < :now")
    void deleteAllExpiredBefore(LocalDateTime now);
}
