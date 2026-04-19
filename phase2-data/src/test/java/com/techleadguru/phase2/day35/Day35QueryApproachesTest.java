package com.techleadguru.phase2.day35;

import com.techleadguru.phase2.Phase2Application;
import com.techleadguru.phase2.day35.Day35QueryApproaches.OrderSearchService;
import com.techleadguru.phase2.shared.Order;
import com.techleadguru.phase2.shared.OrderItemRepository;
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
class Day35QueryApproachesTest {

    @Autowired OrderSearchService orderSearchService;
    @Autowired OrderJpqlRepository orderJpqlRepository;
    @Autowired OrderItemRepository orderItemRepository;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderJpqlRepository.deleteAll();

        // user-A: 3 PENDING orders (status defaults to "PENDING" in Order constructor)
        save3Orders("user-A", new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("300"));
        // user-B: 3 PENDING orders
        save3Orders("user-B", new BigDecimal("50"), new BigDecimal("150"), new BigDecimal("250"));
    }

    private void save3Orders(String userId, BigDecimal t1, BigDecimal t2, BigDecimal t3) {
        orderJpqlRepository.save(new Order(userId, t1));
        orderJpqlRepository.save(new Order(userId, t2));
        orderJpqlRepository.save(new Order(userId, t3));
    }

    @Test
    void jpql_staticQuery_byUserIdAndStatus() {
        List<Order> result = orderSearchService.findByUserAndStatus("user-A", "PENDING");
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(o -> o.getUserId().equals("user-A"));
    }

    @Test
    void specification_dynamicSearch_allFilters() {
        List<Order> result = orderSearchService.searchWithSpec(
            "user-A", "PENDING",
            new BigDecimal("100"), new BigDecimal("250"));

        // user-A has totals 100, 200, 300 — between 100 and 250: 100, 200
        assertThat(result).hasSize(2);
    }

    @Test
    void specification_dynamicSearch_partialFilters() {
        // Only filter by userId — no status, no total range
        List<Order> result = orderSearchService.searchWithSpec("user-B", null, null, null);
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(o -> o.getUserId().equals("user-B"));
    }

    @Test
    void specification_dynamicSearch_noFilters_returnsAll() {
        List<Order> result = orderSearchService.searchWithSpec(null, null, null, null);
        assertThat(result).hasSize(6); // 3 user-A + 3 user-B
    }

    @Test
    void criteriaApi_dynamicSearch_withMinTotal() {
        List<Order> result = orderSearchService.searchWithCriteria(
            "user-A", null, new BigDecimal("200"), null);

        // user-A totals: 100, 200, 300 — >= 200: two results
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(o -> o.getTotal().compareTo(new BigDecimal("200")) >= 0);
    }

    @Test
    void nativeQuery_topOrders_byMinTotal() {
        List<Order> result = orderSearchService.topOrdersForUser(
            "user-A", new BigDecimal("150"), 2);

        // user-A totals: 100, 200, 300 — above 150: 200, 300 — limited to 2
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(o -> o.getTotal().compareTo(new BigDecimal("150")) > 0);
    }

    @Test
    void jpql_aggregate_countAndSum() {
        long count = orderJpqlRepository.countByUserIdJpql("user-A");
        assertThat(count).isEqualTo(3);

        BigDecimal sum = orderJpqlRepository.sumTotalByUserId("user-A");
        // 100 + 200 + 300 = 600
        assertThat(sum.compareTo(new BigDecimal("600"))).isEqualTo(0);
    }
}
