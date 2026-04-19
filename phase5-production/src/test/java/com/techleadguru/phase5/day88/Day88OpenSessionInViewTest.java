package com.techleadguru.phase5.day88;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application.properties")
@Import(Day88OpenSessionInViewTest.JpaConfig.class)
class Day88OpenSessionInViewTest {

    @TestConfiguration
    @EnableJpaRepositories(
            basePackageClasses = Day88OpenSessionInView.class,
            considerNestedRepositories = true
    )
    static class JpaConfig {}

    @Autowired
    private Day88OpenSessionInView.OrderRepository orderRepository;

    private Day88OpenSessionInView.OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new Day88OpenSessionInView.OrderService(orderRepository);
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
    }

    @Test
    void createOrder_persists_order_with_items() {
        Day88OpenSessionInView.Order order = orderService.createOrder(
                "CUST-1", List.of("Laptop", "Mouse"));
        assertThat(order.getId()).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo("CUST-1");
    }

    @Test
    void findOrdersWithItems_returns_orders_for_customer() {
        orderService.createOrder("CUST-2", List.of("Keyboard"));
        orderService.createOrder("CUST-2", List.of("Monitor", "Speakers"));
        orderService.createOrder("CUST-99", List.of("Other"));

        List<Day88OpenSessionInView.Order> orders =
                orderService.findOrdersWithItems("CUST-2");
        assertThat(orders).hasSize(2);
        orders.forEach(o -> assertThat(o.getCustomerId()).isEqualTo("CUST-2"));
    }

    @Test
    void findOrdersWithItems_eager_loads_items_within_transaction() {
        orderService.createOrder("CUST-3", List.of("Desk", "Chair"));

        List<Day88OpenSessionInView.Order> orders =
                orderService.findOrdersWithItems("CUST-3");
        assertThat(orders).isNotEmpty();

        // Items should be accessible (JOIN FETCH inside transaction)
        Day88OpenSessionInView.Order order = orders.get(0);
        assertThat(order.getItems()).isNotEmpty();
        assertThat(order.getItems()).hasSize(2);
    }

    @Test
    void findOrdersWithItems_items_contain_correct_product_names() {
        orderService.createOrder("CUST-4", List.of("Widget", "Gadget"));

        List<Day88OpenSessionInView.Order> orders =
                orderService.findOrdersWithItems("CUST-4");
        assertThat(orders).isNotEmpty();

        List<String> productNames = orders.get(0).getItems().stream()
                .map(Day88OpenSessionInView.OrderItem::getProductName)
                .toList();
        assertThat(productNames).containsExactlyInAnyOrder("Widget", "Gadget");
    }
}
