package com.techleadguru.phase1.day06;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * DAY 6 — @Scope: Singleton vs Prototype — The Injection Bug
 *
 * THE BUG (production data leak):
 *   ShoppingCart is @Scope("prototype") = new instance per request (intended).
 *   OrderService is singleton = created once, lives forever.
 *   @Autowired ShoppingCart in OrderService = Spring injects ONE cart at startup -> shared forever.
 *   Every user modifies the SAME cart. User A sees User B's items. Privacy violation.
 *
 * THREE FIXES (in order of preference):
 *   1. ObjectProvider<ShoppingCart> — call cartProvider.getObject() per method (cleanest)
 *   2. @Lookup method injection — abstract method that Spring overrides (CGLIB)
 *   3. ApplicationContext.getBean() — works but couples to Spring API (avoid)
 *
 * PRODUCTION SCENARIO:
 *   E-commerce app. User A adds items, user B sees User A's items in their cart.
 *   Intermittent in dev (single user), always broken under load.
 *   Root cause: prototype in singleton = singleton wins.
 *
 * HOW TO PROVE IT: Run Day06ScopeTest — it counts instances and verifies same/different.
 */
@Slf4j
public class Day06Scope {

    // ===================================================================================
    // The PROTOTYPE bean — each call should get a fresh instance
    // ===================================================================================

    @Component
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Slf4j
    public static class ShoppingCart {

        private final java.util.List<String> items = new java.util.ArrayList<>();
        private final long id = System.nanoTime(); // unique per instance

        public ShoppingCart() {
            log.info("[ShoppingCart] New cart created: id={}", id);
        }

        public void addItem(String item)   { items.add(item); }
        public void clearCart()             { items.clear(); }
        public java.util.List<String> getItems() { return items; }
        public long getId() { return id; }
    }

    // ===================================================================================
    // THE BUG: Direct @Autowired prototype into singleton = one cart shared by all users
    // ===================================================================================

    @Component("brokenOrderService")
    @Slf4j
    public static class BrokenOrderService {

        @Autowired
        private ShoppingCart sharedCart; // BUG: Spring injects ONE prototype at creation time

        public void addItem(String userId, String item) {
            sharedCart.addItem(userId + ":" + item);
            log.warn("[BUG] Cart id={} now has {} items — shared across all users!",
                    sharedCart.getId(), sharedCart.getItems().size());
        }

        public java.util.List<String> getItems() { return sharedCart.getItems(); }
        public long getCartId() { return sharedCart.getId(); }
    }

    // ===================================================================================
    // FIX 1 (BEST): ObjectProvider — fresh prototype per method call
    // ===================================================================================

    @Component("day06FixedServiceV1")
    @Slf4j
    public static class FixedOrderServiceWithObjectProvider {

        private final ObjectProvider<ShoppingCart> cartProvider;

        public FixedOrderServiceWithObjectProvider(ObjectProvider<ShoppingCart> cartProvider) {
            this.cartProvider = cartProvider;
        }

        public ShoppingCart startNewCart() {
            ShoppingCart freshCart = cartProvider.getObject(); // NEW instance every call
            log.info("[FIX-1] Fresh cart created: id={}", freshCart.getId());
            return freshCart;
        }
    }

    // ===================================================================================
    // FIX 2: @Lookup method injection — Spring CGLIB overrides the method to return fresh bean
    // ===================================================================================

    @Component("day06FixedServiceV2")
    @Slf4j
    public abstract static class FixedOrderServiceWithLookup {

        @Lookup  // Spring generates @Override via CGLIB that calls context.getBean(ShoppingCart.class)
        public abstract ShoppingCart createCart();

        public void processOrder(String userId) {
            ShoppingCart cart = createCart(); // always a new prototype
            log.info("[FIX-2/@Lookup] Fresh cart id={} for user={}", cart.getId(), userId);
        }
    }

    // ===================================================================================
    // Demonstrate the bug fires at startup — ONE cart shared
    // ===================================================================================

    @Configuration
    static class Day06DemoConfig {
        @Bean
        public org.springframework.boot.ApplicationRunner day06Runner(
                BrokenOrderService broken,
                FixedOrderServiceWithObjectProvider fixed1) {
            return args -> {
                System.out.println("=== DAY 6: Scope Bug Demo ===");

                // Simulate 2 users with broken service
                broken.addItem("userA", "Laptop");
                broken.addItem("userB", "Phone");
                System.out.println("[BUG]  UserB's cart has: " + broken.getItems()); // shows BOTH items!

                // Simulate 2 users with fixed service
                var cartA = fixed1.startNewCart();
                var cartB = fixed1.startNewCart();
                cartA.addItem("Laptop");
                cartB.addItem("Phone");
                System.out.println("[FIX1] CartA: " + cartA.getItems() + " | CartB: " + cartB.getItems());
                System.out.println("[FIX1] Same cart? " + (cartA.getId() == cartB.getId())); // false = correct

                System.out.println("=============================");
            };
        }
    }
}
