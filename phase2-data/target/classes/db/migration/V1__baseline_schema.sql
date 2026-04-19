-- Day 40: Flyway migration demo
-- Flyway runs these in order on startup.
-- V1 = baseline schema

CREATE TABLE IF NOT EXISTS users (
    id          VARCHAR(36) PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    balance     DECIMAL(10,2) DEFAULT 0.00, -- Day 26, 29, 30, 42
    version     BIGINT DEFAULT 0,           -- Day 42: @Version optimistic locking
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
    id          VARCHAR(36) PRIMARY KEY,
    user_id     VARCHAR(36) NOT NULL,       -- no FK constraint: tests use arbitrary userId strings
    total       DECIMAL(10,2) NOT NULL,
    status      VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    version     BIGINT DEFAULT 0,           -- Day 42: @Version optimistic locking
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
    id          VARCHAR(36) PRIMARY KEY,
    order_id    VARCHAR(36) NOT NULL REFERENCES orders(id),
    product     VARCHAR(255) NOT NULL,
    quantity    INT NOT NULL,
    price       DECIMAL(10,2) NOT NULL
);

-- Index to prevent slow queries (Day 41)
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
