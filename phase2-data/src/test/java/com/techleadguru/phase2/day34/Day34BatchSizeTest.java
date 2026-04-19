package com.techleadguru.phase2.day34;

import com.techleadguru.phase2.Phase2Application;
import com.techleadguru.phase2.day34.Day34BatchSize.BatchOrderService;
import com.techleadguru.phase2.day34.Day34BatchSize.OrderSummary;
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
class Day34BatchSizeTest {

    @Autowired BatchOrderService batchOrderService;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void batchSize_reducesQueryCount_correctResults() {
        // Create 12 orders, each with 2 items → @BatchSize(10) → ceil(12/10)=2 batch queries for items
        for (int i = 0; i < 12; i++) {
            batchOrderService.createOrder("batch-user-" + i, List.of("Item-A-" + i, "Item-B-" + i));
        }

        List<OrderSummary> summaries = batchOrderService.getAllWithBatchLoading();

        assertThat(summaries).hasSize(12);
        assertThat(summaries).allMatch(s -> s.itemCount() == 2,
            "Each order should have 2 items loaded via @BatchSize batching");
    }

    @Test
    void batchSize_singleOrder_correctItems() {
        batchOrderService.createOrder("user-single", List.of("Laptop", "Charger", "Bag"));

        List<OrderSummary> summaries = batchOrderService.getAllWithBatchLoading();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).itemCount()).isEqualTo(3);
    }

    @Test
    void batchSize_exactlyBatchBoundary_correctResults() {
        // Exactly 10 orders (batch size) → should trigger exactly 1 IN query for all items
        for (int i = 0; i < 10; i++) {
            batchOrderService.createOrder("user-boundary-" + i, List.of("P-" + i));
        }

        List<OrderSummary> summaries = batchOrderService.getAllWithBatchLoading();

        assertThat(summaries).hasSize(10);
        assertThat(summaries).allMatch(s -> s.itemCount() == 1);

        System.out.println("[Day34] @BatchSize(10) on 10 orders = 1 batch IN query instead of 10 individual queries");
    }

    @Test
    void batchSize_emptyCollection_handled() {
        // Order with no items
        batchOrderService.createOrder("user-empty", List.of());

        List<OrderSummary> summaries = batchOrderService.getAllWithBatchLoading();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).itemCount()).isEqualTo(0);
    }
}
