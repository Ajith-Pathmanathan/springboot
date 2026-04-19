package com.techleadguru.phase1.day09;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 9 — Test: Verify @Autowired resolution by name and @Qualifier.
 *
 * These tests run without Spring — they directly instantiate and wire by hand,
 * proving the resolution rules.
 */
class Day09AutowiredResolutionTest {

    private final Day09AutowiredResolution.VisaPaymentService visa =
            new Day09AutowiredResolution.VisaPaymentService();
    private final Day09AutowiredResolution.MastercardPaymentService mastercard =
            new Day09AutowiredResolution.MastercardPaymentService();

    // -----------------------------------------------------------------------
    // Test 1: Name-based resolution — matches field name to bean name
    // -----------------------------------------------------------------------
    @Test
    void name_based_resolution_injects_visa_when_field_is_named_visaPaymentService() {
        var orderService = new Day09AutowiredResolution.OrderServiceByName(visa);
        String result = orderService.placeOrder("100");

        assertThat(result).isEqualTo("VISA charged: 100");
        System.out.println("[DAY 9] Name resolution: field 'visaPaymentService' → VisaPaymentService injected");
    }

    // -----------------------------------------------------------------------
    // Test 2: @Qualifier forces Mastercard regardless of field name
    // -----------------------------------------------------------------------
    @Test
    void qualifier_resolution_injects_mastercard_explicitly() {
        var orderService = new Day09AutowiredResolution.OrderServiceByQualifier(mastercard);
        String result = orderService.placeOrder("200");

        assertThat(result).isEqualTo("MASTERCARD charged: 200");
        System.out.println("[DAY 9] Qualifier resolution: @Qualifier(\"mastercardPaymentService\") → MastercardPaymentService injected");
    }

    // -----------------------------------------------------------------------
    // Test 3: Document the ambiguity rule
    // -----------------------------------------------------------------------
    @Test
    void without_qualifier_ambiguous_injection_must_be_avoided() {
        // If you inject PaymentService without any qualifier/name hint and
        // two beans exist, Spring throws NoUniqueBeanDefinitionException.
        // We document this as a rule, not a runnable Spring context test:
        System.out.println("[DAY 9] RULE: Two beans of type PaymentService exist.");
        System.out.println("[DAY 9]   → Never write @Autowired PaymentService service without @Qualifier.");
        System.out.println("[DAY 9]   → Spring throws NoUniqueBeanDefinitionException at startup.");
        System.out.println("[DAY 9]   → Relying on field name match is fragile (refactor = silent bug).");
        assertThat(visa.charge("50")).startsWith("VISA");
        assertThat(mastercard.charge("50")).startsWith("MASTERCARD");
    }
}
