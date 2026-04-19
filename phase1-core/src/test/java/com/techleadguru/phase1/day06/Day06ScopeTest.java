package com.techleadguru.phase1.day06;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 6 Test: Verify the scope bug and all three fixes with assertions.
 */
@SpringBootTest(classes = com.techleadguru.phase1.Phase1Application.class)
class Day06ScopeTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    Day06Scope.BrokenOrderService brokenService;

    @Autowired
    Day06Scope.FixedOrderServiceWithObjectProvider fixedService;

    // -----------------------------------------------------------------------
    // Test 1: REPRODUCE THE BUG — same cart across calls
    // -----------------------------------------------------------------------
    @Test
    void broken_service_shares_same_cart_across_all_users() {
        // Both users add items to the same (wrong!) singleton cart
        brokenService.addItem("userA", "Laptop");
        brokenService.addItem("userB", "Phone");

        // BUG: cart contains items from BOTH users
        assertThat(brokenService.getItems())
                .as("BUG: same cart is shared — user A sees user B's items")
                .hasSizeGreaterThan(1);

        System.out.println("[DAY 6 BUG CONFIRMED] Shared cart items: " + brokenService.getItems());

        // Clean up for other tests
        brokenService.getItems().clear();
    }

    // -----------------------------------------------------------------------
    // Test 2: FIX 1 — ObjectProvider gives a fresh cart each time
    // -----------------------------------------------------------------------
    @Test
    void fixed_service_with_ObjectProvider_creates_separate_cart_per_call() {
        var cartA = fixedService.startNewCart();
        var cartB = fixedService.startNewCart();

        cartA.addItem("Laptop");
        cartB.addItem("Phone");

        // FIX: each user gets their own cart
        assertThat(cartA.getId())
                .as("FIX: different cart instances — different IDs")
                .isNotEqualTo(cartB.getId());

        assertThat(cartA.getItems()).containsExactly("Laptop");
        assertThat(cartB.getItems()).containsExactly("Phone");

        System.out.println("[DAY 6 FIX CONFIRMED] CartA: " + cartA.getItems() + ", CartB: " + cartB.getItems());
    }

    // -----------------------------------------------------------------------
    // Test 3: Validate context itself — prototype creates new instance per getBean()
    // -----------------------------------------------------------------------
    @Test
    void prototype_scope_creates_new_instance_per_getBean_call() {
        var cart1 = context.getBean(Day06Scope.ShoppingCart.class);
        var cart2 = context.getBean(Day06Scope.ShoppingCart.class);

        assertThat(cart1).isNotSameAs(cart2);
        System.out.println("[DAY 6] Prototype confirmed: cart1 id=" + cart1.getId() + " != cart2 id=" + cart2.getId());
    }

    // -----------------------------------------------------------------------
    // Test 4: Singleton scope creates the same instance every time
    // -----------------------------------------------------------------------
    @Test
    void singleton_scope_returns_same_instance_every_getBean_call() {
        var s1 = context.getBean("brokenOrderService", Day06Scope.BrokenOrderService.class);
        var s2 = context.getBean("brokenOrderService", Day06Scope.BrokenOrderService.class);

        assertThat(s1).isSameAs(s2);
        System.out.println("[DAY 6] Singleton confirmed: same instance returned twice");
    }
}
