package com.techleadguru.phase2.day33;

import com.techleadguru.phase2.Phase2Application;
import com.techleadguru.phase2.day33.Day33EntityGraph.OrderSummaryServiceEntityGraph;
import com.techleadguru.phase2.day33.Day33EntityGraph.OrderSummary;
import com.techleadguru.phase2.shared.Order;
import com.techleadguru.phase2.shared.OrderItem;
import com.techleadguru.phase2.shared.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Phase2Application.class)
@ActiveProfiles("test")
class Day33EntityGraphTest {

    @Autowired OrderSummaryServiceEntityGraph service;
    @Autowired OrderEntityGraphRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
    }

    private Order saveOrder(String userId, List<String> products) {
        Order order = new Order(userId, new BigDecimal("29.99"));
        products.forEach(p -> order.addItem(new OrderItem(p, 1, new BigDecimal("9.99"))));
        return orderRepository.save(order);
    }

    @Test
    void entityGraph_loadsItemsEagerly_noNPlusOne() {
        saveOrder("user-1", List.of("Laptop", "Charger"));
        saveOrder("user-2", List.of("Phone", "Case", "Screen Protector"));
        saveOrder("user-3", List.of("Tablet"));

        List<OrderSummary> summaries = service.getAllOrderSummaries();

        assertThat(summaries).hasSize(3);
        // Verify items were loaded
        assertThat(summaries).anySatisfy(s -> assertThat(s.itemCount()).isEqualTo(2)); // user-1
        assertThat(summaries).anySatisfy(s -> assertThat(s.itemCount()).isEqualTo(3)); // user-2
        assertThat(summaries).anySatisfy(s -> assertThat(s.itemCount()).isEqualTo(1)); // user-3
    }

    @Test
    void entityGraph_findByUserId_worksWithDerivedQuery() {
        saveOrder("user-A", List.of("Item1", "Item2"));
        saveOrder("user-A", List.of("Item3"));
        saveOrder("user-B", List.of("Other"));

        List<OrderSummary> result = service.getOrdersForUser("user-A");

        assertThat(result).hasSize(2);
        assertThat(result.stream().mapToInt(OrderSummary::itemCount).sum()).isEqualTo(3);
    }

    @Test
    void entityGraph_findById_loadsItemsEagerly() {
        Order saved = saveOrder("user-Z", List.of("A", "B", "C"));

        Optional<OrderSummary> result = service.getOrderById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().itemCount()).isEqualTo(3);
    }

    @Test
    void entityGraph_withPageable_worksCorrectly() {
        // Create 5 orders
        for (int i = 0; i < 5; i++) {
            saveOrder("user-page-" + i, List.of("Product-" + i, "Extra-" + i));
        }

        // Get first page of 3 — @EntityGraph + Pageable should NOT trigger in-memory pagination
        List<OrderSummary> page1 = service.getOrderSummariesPaginated(PageRequest.of(0, 3));

        assertThat(page1).hasSize(3);
        assertThat(page1).allMatch(s -> s.itemCount() == 2,
            "Each paged order should have 2 items loaded via @EntityGraph + Pageable");

        // Second page
        List<OrderSummary> page2 = service.getOrderSummariesPaginated(PageRequest.of(1, 3));
        assertThat(page2).hasSize(2); // remaining 2 orders
    }
}
