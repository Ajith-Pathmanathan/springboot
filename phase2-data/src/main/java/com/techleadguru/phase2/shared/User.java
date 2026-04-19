package com.techleadguru.phase2.shared;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Shared User entity used across Phase 2 day exercises.
 * Maps to the 'users' table created by Flyway V1 migration.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    private String id;

    @Column(nullable = false)
    private String email;

    @Column
    private String name;

    @Column
    private BigDecimal balance;

    @Version
    private Long version;

    public User() {}

    public User(String email, String name) {
        this.id = UUID.randomUUID().toString();
        this.email = email;
        this.name = name;
        this.balance = BigDecimal.ZERO;
    }

    public User(String email, String name, BigDecimal balance) {
        this.id = UUID.randomUUID().toString();
        this.email = email;
        this.name = name;
        this.balance = balance;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public BigDecimal getBalance() { return balance; }
    public Long getVersion() { return version; }
    public void setName(String name) { this.name = name; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
