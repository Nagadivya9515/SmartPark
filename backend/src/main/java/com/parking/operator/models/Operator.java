package com.parking.operator.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "operators")
public class Operator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_id", nullable = false, unique = true, length = 20)
    private String operatorId;      // OP001, OP002

    @Column(nullable = false)
    private String password;        // BCrypt hashed

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "gate_name", length = 20)
    private String gateName;        // Gate 1, Gate 2

    @Column(name = "lot_name")
    private String lotName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperatorRole role;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum OperatorRole { ENTRY_OPERATOR, EXIT_OPERATOR, SUPERVISOR }

    public Operator() {}

    public Long getId()                { return id; }
    public String getOperatorId()      { return operatorId; }
    public String getPassword()        { return password; }
    public String getFullName()        { return fullName; }
    public String getGateName()        { return gateName; }
    public String getLotName()         { return lotName; }
    public OperatorRole getRole()      { return role; }
    public Boolean getActive()         { return active; }
    public LocalDateTime getLastLogin(){ return lastLogin; }
    public LocalDateTime getCreatedAt(){ return createdAt; }

    public void setId(Long id)                    { this.id = id; }
    public void setOperatorId(String o)           { this.operatorId = o; }
    public void setPassword(String p)             { this.password = p; }
    public void setFullName(String f)             { this.fullName = f; }
    public void setGateName(String g)             { this.gateName = g; }
    public void setLotName(String l)              { this.lotName = l; }
    public void setRole(OperatorRole r)           { this.role = r; }
    public void setActive(Boolean a)              { this.active = a; }
    public void setLastLogin(LocalDateTime l)     { this.lastLogin = l; }
}