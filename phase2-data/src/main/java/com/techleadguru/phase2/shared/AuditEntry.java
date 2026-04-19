package com.techleadguru.phase2.shared;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Shared AuditEntry entity used in Days 23+ for propagation demos.
 * Maps to the 'audit_log' table created by Flyway V2 migration.
 */
@Entity
@Table(name = "audit_log")
public class AuditEntry {

    @Id
    private String id;

    @Column(nullable = false)
    private String action;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "survived_rollback")
    private boolean survivedRollback;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public AuditEntry() {}

    public AuditEntry(String action, String resourceId) {
        this.id = UUID.randomUUID().toString();
        this.action = action;
        this.resourceId = resourceId;
        this.survivedRollback = false;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getAction() { return action; }
    public String getResourceId() { return resourceId; }
    public boolean isSurvivedRollback() { return survivedRollback; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setSurvivedRollback(boolean v) { this.survivedRollback = v; }
}
