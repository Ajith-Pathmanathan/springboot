package com.techleadguru.phase2.shared;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared Order entity used across Phase 2 day exercises (Days 22-42).
 * Maps to the 'orders' table created by Flyway V1 migration.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(nullable = false)
    private String status;

    @Version
    private Long version;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 10)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {}

    public Order(String userId, BigDecimal total) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.total = total;
        this.status = "PENDING";
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public BigDecimal getTotal() { return total; }
    public String getStatus() { return status; }
    public Long getVersion() { return version; }
    public List<OrderItem> getItems() { return items; }
    public void setStatus(String status) { this.status = status; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public void addItem(OrderItem item) { items.add(item); item.setOrder(this); }
}
