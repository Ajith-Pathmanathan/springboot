package com.techleadguru.phase1.day03;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 3 — Test: Verify the exact init/destroy order.
 * This test makes the lifecycle order concrete and verifiable — not just logs.
 */
class Day03LifecycleOrderTest {

    @Test
    void lifecycle_order_is_constructor_then_postConstruct_then_afterPropertiesSet_then_customInit() {
        List<String> order = new ArrayList<>();

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.registerBean("tracked", TrackedBean.class, () -> new TrackedBean(order));
        ctx.refresh();

        // Init assertions
        assertThat(order).containsExactly(
                "constructor",
                "postConstruct",
                "afterPropertiesSet"
        );
        System.out.println("[DAY 3] Init order confirmed: " + order);

        // Trigger destroy
        ctx.close();

        assertThat(order).containsExactly(
                "constructor",
                "postConstruct",
                "afterPropertiesSet",
                "preDestroy",
                "destroy"
        );
        System.out.println("[DAY 3] Full lifecycle order confirmed: " + order);
    }

    static class TrackedBean implements org.springframework.beans.factory.InitializingBean,
            org.springframework.beans.factory.DisposableBean {

        private final List<String> order;

        TrackedBean(List<String> order) {
            this.order = order;
            order.add("constructor");
        }

        @jakarta.annotation.PostConstruct
        public void postConstruct() {
            order.add("postConstruct");
        }

        @Override
        public void afterPropertiesSet() {
            order.add("afterPropertiesSet");
        }

        @jakarta.annotation.PreDestroy
        public void preDestroy() {
            order.add("preDestroy");
        }

        @Override
        public void destroy() {
            order.add("destroy");
        }
    }

    /**
     * PRODUCTION RULE TEST — documented as a reminder, not executable code.
     *
     * @PostConstruct trap: DB connection opened → Config Server not bootstrapped yet → "${db.url}" literal
     * ApplicationRunner safe: runs after full ApplicationContext refresh including Config Server bootstrap
     */
    @Test
    void document_production_rule_startup_io_belongs_in_ApplicationRunner() {
        System.out.println("=== DAY 3 PRODUCTION RULE ===");
        System.out.println("NEVER open DB connections, call HTTP APIs, or access external systems in @PostConstruct.");
        System.out.println("REASON: Spring Cloud Config Server properties are NOT yet injected at @PostConstruct time.");
        System.out.println("FIX:    Move all startup I/O to ApplicationRunner.run() — guaranteed post-refresh.");
        System.out.println("==============================");
    }
}
