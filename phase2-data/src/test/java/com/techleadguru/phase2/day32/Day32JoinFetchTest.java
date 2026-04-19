package com.techleadguru.phase2.day32;

import com.techleadguru.phase2.Phase2Application;
import com.techleadguru.phase2.day31.Day31NPlusOneProblem.OrderSummary;
import com.techleadguru.phase2.day32.Day32JoinFetch.OrderSummaryServiceFixed;
import com.techleadguru.phase2.shared.Order;
import com.techleadguru.phase2.shared.OrderItem;
import com.techleadguru.phase2.shared.OrderItemRepository;
import com.techleadguru.phase2.shared.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Phase2Application.class)
@ActiveProfiles("test")
class Day32JoinFetchTest {

    @Autowired OrderSummaryServiceFixed orderSummaryServiceFixed;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
    }

    private Order createOrderWithItems(String userId, List<String> products) {
        Order order = new Order(userId, new BigDecimal("49.99"));
        products.forEach(p -> order.addItem(new OrderItem(p, 1, new BigDecimal("9.99"))));
        return orderRepository.save(order);
    }

    @Test
    void joinFetch_loadsAllOrdersInOneQuery() {
        // 3 orders, 2 items each
        createOrderWithItems("user-1", List.of("Book", "Pen"));
        createOrderWithItems("user-2", List.of("Notebook", "Pencil"));
        createOrderWithItems("user-3", List.of("Ruler", "Eraser"));

        // JOIN FETCH: 1 query for everything
        List<OrderSummary> summaries = orderSummaryServiceFixed.getAllOrderSummariesFixed();

        assertThat(summaries).hasSize(3);
        assertThat(summaries).allMatch(s -> s.itemCount() == 2,
            "Each order should have 2 items loaded via JOIN FETCH");

        System.out.println("[Day32] JOIN FETCH result: " + summaries);
    }

    @Test
    void joinFetch_specificUser_stillOneQuery() {
        createOrderWithItems("user-X", List.of("Item1", "Item2", "Item3"));
        createOrderWithItems("user-X", List.of("Item4"));
        createOrderWithItems("user-Y", List.of("Other"));

        List<OrderSummary> result = orderSummaryServiceFixed.getOrderSummariesForUser("user-X");

        assertThat(result).hasSize(2);
        // First order has 3 items, second has 1
        assertThat(result.stream().mapToInt(OrderSummary::itemCount).sum()).isEqualTo(4);
    }

    @Test
    void joinFetch_distinct_deduplicatesParent() {
        // Create 1 order with 5 items — JOIN produces 5 rows.
        // DISTINCT in JPQL should still return 1 Order object.
        Order order = new Order("user-D", new BigDecimal("100"));
        for (int i = 0; i < 5; i++) {
            order.addItem(new OrderItem("Product-" + i, 1, new BigDecimal("20")));
        }
        orderRepository.save(order);

        List<Order> orders = orderRepository.findAllWithItems();

        assertThat(orders).hasSize(1); // DISTINCT works — no duplicate Order objects
        assertThat(orders.get(0).getItems()).hasSize(5);
    }
}
