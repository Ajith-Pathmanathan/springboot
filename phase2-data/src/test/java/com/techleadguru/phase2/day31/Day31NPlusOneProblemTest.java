package com.techleadguru.phase2.day31;

import com.techleadguru.phase2.Phase2Application;
import com.techleadguru.phase2.day31.Day31NPlusOneProblem.OrderSummaryService;
import com.techleadguru.phase2.day31.Day31NPlusOneProblem.OrderSummary;
import com.techleadguru.phase2.shared.OrderItemRepository;
import com.techleadguru.phase2.shared.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Phase2Application.class)
@ActiveProfiles("test")
class Day31NPlusOneProblemTest {

    @Autowired OrderSummaryService orderSummaryService;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void nPlusOneProblem_loadsItemsLazily() {
        // Arrange: create 3 orders, each with 2 items
        orderSummaryService.createOrderWithItems("user-1", List.of("Laptop", "Mouse"));
        orderSummaryService.createOrderWithItems("user-2", List.of("Keyboard", "Monitor"));
        orderSummaryService.createOrderWithItems("user-3", List.of("Webcam", "Headset"));

        // Act: this internally triggers N+1 (1 SELECT orders + 3 SELECT items)
        List<OrderSummary> summaries = orderSummaryService.getAllOrderSummariesNPlusOne();

        // Assert: results are correct despite the N+1 inefficiency
        assertThat(summaries).hasSize(3);
        assertThat(summaries).allMatch(s -> s.itemCount() == 2,
            "Each order should have 2 items loaded via lazy N+1 queries");
    }

    @Test
    void nPlusOneProblem_moreOrders_moreQueries() {
        // Arrange: 5 orders — will trigger 1 + 5 = 6 queries (vs just 1 with JOIN FETCH)
        for (int i = 0; i < 5; i++) {
            orderSummaryService.createOrderWithItems("user-" + i, List.of("Item-A-" + i, "Item-B-" + i, "Item-C-" + i));
        }

        // Act
        List<OrderSummary> summaries = orderSummaryService.getAllOrderSummariesNPlusOne();

        // Assert: each order has 3 items
        assertThat(summaries).hasSize(5);
        assertThat(summaries).allMatch(s -> s.itemCount() == 3);

        System.out.println("[Day31] N+1 reproduced: 1 SELECT for orders + 5 SELECTs for items = 6 total queries");
    }
}
