package com.techleadguru.phase2.shared;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Shared OrderItem entity used across Phase 2 day exercises.
 * Maps to the 'order_items' table created by Flyway V1 migration.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private String product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal price;

    public OrderItem() {}

    public OrderItem(String product, int quantity, BigDecimal price) {
        this.id = UUID.randomUUID().toString();
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }

    public String getId() { return id; }
    public String getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public Order getOrder() { return order; }
    public void setOrder(Order o) { this.order = o; }
}
