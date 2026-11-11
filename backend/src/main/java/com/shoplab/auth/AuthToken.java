package com.shoplab.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** A login session: a random token that belongs to one user. */
@Entity
@Table(name = "auth_tokens")
public class AuthToken {

    @Id
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at")
    private Instant createdAt;

    protected AuthToken() {
    }

    public AuthToken(String token, User user) {
        this.token = token;
        this.user = user;
        this.createdAt = Instant.now();
    }

    public String getToken() { return token; }
    public User getUser() { return user; }
    public Instant getCreatedAt() { return createdAt; }
}
