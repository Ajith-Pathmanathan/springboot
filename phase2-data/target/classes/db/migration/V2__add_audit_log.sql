-- Day 23: Audit log table for REQUIRES_NEW propagation demo
-- Also used by Days 24-28

CREATE TABLE IF NOT EXISTS audit_log (
    id                 VARCHAR(36) PRIMARY KEY,
    action             VARCHAR(255) NOT NULL,
    resource_id        VARCHAR(36),
    survived_rollback  BOOLEAN DEFAULT TRUE,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
