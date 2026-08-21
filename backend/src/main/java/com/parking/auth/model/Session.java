package com.parking.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token", nullable = false, unique = true, length = 500)
    private String refreshToken;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "device_info", length = 200)
    private String deviceInfo;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Default Constructor for JPA
    public Session() {}

    // Private Constructor for Builder
    private Session(SessionBuilder builder) {
        this.id = builder.id;
        this.user = builder.user;
        this.refreshToken = builder.refreshToken;
        this.expiryDate = builder.expiryDate;
        this.deviceInfo = builder.deviceInfo;
        this.createdAt = builder.createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }

    // Static method to start the builder
    public static SessionBuilder builder() {
        return new SessionBuilder();
    }

    // --- Manual Builder Class ---
    public static class SessionBuilder {
        private Long id;
        private User user;
        private String refreshToken;
        private LocalDateTime expiryDate;
        private String deviceInfo;
        private LocalDateTime createdAt;

        public SessionBuilder id(Long id) { this.id = id; return this; }
        public SessionBuilder user(User user) { this.user = user; return this; }
        public SessionBuilder refreshToken(String refreshToken) { this.refreshToken = refreshToken; return this; }
        public SessionBuilder expiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; return this; }
        public SessionBuilder deviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; return this; }
        public SessionBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Session build() {
            return new Session(this);
        }
    }

    // --- Standard Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}