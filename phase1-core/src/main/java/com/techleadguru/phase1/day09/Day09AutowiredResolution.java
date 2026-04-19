package com.techleadguru.phase1.day09;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

/**
 * DAY 9 — @Autowired Resolution: Type → Name → @Qualifier
 *
 * THE ALGORITHM Spring uses when you write @Autowired:
 *   Step 1 — By TYPE: Find all beans matching the declared type.
 *             If exactly 1 match → inject it. Done.
 *   Step 2 — By NAME: If multiple candidates found, match against the field/param name.
 *             e.g. @Autowired PaymentService visaPaymentService → looks for bean named "visaPaymentService".
 *   Step 3 — By @Qualifier: If name match fails or is ambiguous, @Qualifier("beanName") forces the pick.
 *   FAIL: If 0 matches AND @Autowired(required=true) → NoSuchBeanDefinitionException.
 *         If 2+ matches AND no name/qualifier match → NoUniqueBeanDefinitionException.
 *
 * PRODUCTION SCENARIO — Silent wrong bean injection:
 *   Two DataSource implementations: primaryDataSource, secondaryDataSource.
 *   Dev writes @Autowired DataSource dataSource — name "dataSource" matches neither.
 *   Spring picks one via ordering (non-deterministic). Wrong DB queries go to secondary.
 *   Data corruption in production. Weeks before discovered.
 *   FIX: Always @Qualifier when multiple beans of same type exist.
 *
 * RULE: When you have 2+ beans of the same type, ALWAYS use @Qualifier or @Primary.
 *       Relying on name matching is fragile — any refactor breaks it silently.
 */
@Slf4j
public class Day09AutowiredResolution {

    // ===================================================================================
    // Setup: two implementations of the same interface
    // ===================================================================================

    public interface PaymentService {
        String charge(String amount);
    }

    @Service("visaPaymentService")
    public static class VisaPaymentService implements PaymentService {
        @Override
        public String charge(String amount) {
            return "VISA charged: " + amount;
        }
    }

    @Service("mastercardPaymentService")
    public static class MastercardPaymentService implements PaymentService {
        @Override
        public String charge(String amount) {
            return "MASTERCARD charged: " + amount;
        }
    }

    // ===================================================================================
    // Step 2: Resolution by field NAME matches bean name
    // ===================================================================================

    @Service("orderServiceByName")
    @Slf4j
    public static class OrderServiceByName {

        private final PaymentService visaPaymentService; // field name = bean name → Spring resolves correctly

        public OrderServiceByName(PaymentService visaPaymentService) {
            this.visaPaymentService = visaPaymentService;
            log.info("[Day09] OrderServiceByName wired: {}", visaPaymentService.getClass().getSimpleName());
        }

        public String placeOrder(String amount) {
            return visaPaymentService.charge(amount);
        }
    }

    // ===================================================================================
    // Step 3: Resolution by @Qualifier — explicit, always correct, refactor-safe
    // ===================================================================================

    @Service("orderServiceByQualifier")
    @Slf4j
    public static class OrderServiceByQualifier {

        private final PaymentService paymentService;

        public OrderServiceByQualifier(@Qualifier("mastercardPaymentService") PaymentService paymentService) {
            this.paymentService = paymentService;
            log.info("[Day09] OrderServiceByQualifier wired: {}", paymentService.getClass().getSimpleName());
        }

        public String placeOrder(String amount) {
            return paymentService.charge(amount);
        }
    }

    // ===================================================================================
    // Configuration to register both services
    // ===================================================================================

    @Configuration
    public static class Day09Config {

        @Bean
        public VisaPaymentService visaPaymentService() {
            return new VisaPaymentService();
        }

        @Bean
        public MastercardPaymentService mastercardPaymentService() {
            return new MastercardPaymentService();
        }

        @Bean
        public OrderServiceByName orderServiceByName(
                @Qualifier("visaPaymentService") PaymentService visaPaymentService) {
            return new OrderServiceByName(visaPaymentService);
        }

        @Bean
        public OrderServiceByQualifier orderServiceByQualifier(
                @Qualifier("mastercardPaymentService") PaymentService paymentService) {
            return new OrderServiceByQualifier(paymentService);
        }
    }
}
