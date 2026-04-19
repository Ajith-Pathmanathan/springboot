-- ShedLock table (Day 67) — created before Hibernate DDL runs
-- H2-compatible syntax
CREATE TABLE IF NOT EXISTS shedlock (
    name     VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP  NOT NULL,
    locked_at  TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
