package com.parking.admin.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_users")
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;           // admin@smartpark.com

    @Column(nullable = false)
    private String password;           // BCrypt

    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminRole role;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum AdminRole { SUPER_ADMIN, ADMIN }

    public AdminUser() {}

    public Long getId()                  { return id; }
    public String getUsername()          { return username; }
    public String getPassword()          { return password; }
    public String getFullName()          { return fullName; }
    public AdminRole getRole()           { return role; }
    public Boolean getActive()           { return active; }
    public LocalDateTime getLastLogin()  { return lastLogin; }
    public LocalDateTime getCreatedAt()  { return createdAt; }

    public void setId(Long id)                   { this.id = id; }
    public void setUsername(String u)            { this.username = u; }
    public void setPassword(String p)            { this.password = p; }
    public void setFullName(String f)            { this.fullName = f; }
    public void setRole(AdminRole r)             { this.role = r; }
    public void setActive(Boolean a)             { this.active = a; }
    public void setLastLogin(LocalDateTime l)    { this.lastLogin = l; }
}