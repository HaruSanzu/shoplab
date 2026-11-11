package com.shoplab.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    private String name;

    /** Locked users can't log in ("locked@shoplab.test"). */
    private boolean locked;

    /** Login takes 3 seconds, to practise explicit waits ("slow@shoplab.test"). */
    @Column(name = "slow_login")
    private boolean slowLogin;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(String email, String passwordHash, String name) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getName() { return name; }
    public boolean isLocked() { return locked; }
    public boolean isSlowLogin() { return slowLogin; }
    public Instant getCreatedAt() { return createdAt; }
}
