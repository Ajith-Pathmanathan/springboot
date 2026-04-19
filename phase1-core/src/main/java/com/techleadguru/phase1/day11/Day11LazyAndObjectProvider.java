package com.techleadguru.phase1.day11;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * DAY 11 — @Lazy and ObjectProvider
 *
 * PROBLEM 1 — Eager initialization cost:
 *   Spring creates ALL singleton beans at startup by default.
 *   A ReportGeneratorService connects to S3, initialises PDF libraries, warms caches.
 *   Takes 4 seconds to initialise. Application startup takes 4+ extra seconds.
 *   Most requests never generate a PDF. Startup cost wasted.
 *   FIX: @Lazy on the bean or injection point — Spring creates it on first use.
 *
 * PROBLEM 2 — Circular dependency at startup:
 *   ServiceA depends on ServiceB, ServiceB depends on ServiceA.
 *   Spring Boot 2.6+ throws BeanCurrentlyInCreationException at startup.
 *   @Lazy on one injection point breaks the cycle — Spring injects a proxy,
 *   creates the real bean on first method call.
 *
 * PROBLEM 3 — Optional or multiple beans:
 *   ObjectProvider<T> is the Spring way to inject dependencies that:
 *     (a) may not exist (optional), or
 *     (b) have multiple instances (iterate them), or
 *     (c) are prototype-scoped (get fresh instance per call via getObject()).
 *   Unlike @Autowired(required=false), ObjectProvider<T> does not fail at startup.
 *   getIfAvailable() returns null if no bean found. getIfUnique() returns null if multiple found.
 *
 * PRODUCTION SCENARIO — 4-second startup for a PDF service used once a week:
 *   Finance team runs weekly PDF export. Service connected to external PDF vendor at startup.
 *   During vendor outage: application won't start at all.
 *   FIX: @Lazy on the service. Startup succeeds. PDF generation fails gracefully at use time.
 */
@Slf4j
public class Day11LazyAndObjectProvider {

    // ===================================================================================
    // Expensive service — should not be created at startup
    // ===================================================================================

    @Component
    @Lazy // Created only on first use
    @Slf4j
    public static class ExpensivePdfService {

        public ExpensivePdfService() {
            // Simulate slow initialisation (in tests we don't actually sleep)
            log.info("[Day11] ExpensivePdfService instantiated — this is expensive!");
        }

        public String generateReport(String reportId) {
            return "PDF[" + reportId + "]";
        }
    }

    // ===================================================================================
    // Service that lazily injects the expensive one
    // ===================================================================================

    @Component
    @Slf4j
    public static class ReportController {

        private final ObjectProvider<ExpensivePdfService> pdfServiceProvider; // NOT created at injection time

        public ReportController(ObjectProvider<ExpensivePdfService> pdfServiceProvider) {
            this.pdfServiceProvider = pdfServiceProvider;
            // ExpensivePdfService is NOT yet created here
            log.info("[Day11] ReportController created — PdfService NOT yet instantiated");
        }

        public String generateReport(String id) {
            // Created HERE on first call
            return pdfServiceProvider.getObject().generateReport(id);
        }
    }

    // ===================================================================================
    // ObjectProvider for optional dependencies
    // ===================================================================================

    public interface SlackNotifier {
        void send(String message);
    }

    @Component
    @Slf4j
    public static class AlertService {

        private final ObjectProvider<SlackNotifier> slackProvider;

        public AlertService(ObjectProvider<SlackNotifier> slackProvider) {
            this.slackProvider = slackProvider;
        }

        public void sendAlert(String message) {
            // getIfAvailable() returns null if SlackNotifier bean doesn't exist — no NPE, no exception
            SlackNotifier slack = slackProvider.getIfAvailable();
            if (slack != null) {
                slack.send(message);
                log.info("[Day11] Alert sent via Slack: {}", message);
            } else {
                log.warn("[Day11] No SlackNotifier bean found — alert logged only: {}", message);
            }
        }

        public boolean hasSlackBean() {
            return slackProvider.getIfAvailable() != null;
        }
    }

    // ===================================================================================
    // @Lazy to break circular dependency
    // ===================================================================================

    @Component
    @Slf4j
    public static class ServiceA {
        private final ServiceB serviceB;

        public ServiceA(@Lazy ServiceB serviceB) { // @Lazy breaks the circular dep
            this.serviceB = serviceB;
            log.info("[Day11] ServiceA created");
        }

        public String hello() { return "A->B:" + serviceB.greet(); }
    }

    @Component
    @Slf4j
    public static class ServiceB {
        private final ServiceA serviceA;

        public ServiceB(ServiceA serviceA) {
            this.serviceA = serviceA;
            log.info("[Day11] ServiceB created");
        }

        public String greet() { return "ServiceB.greet()"; }
        public String ping() { return "B->A:" + serviceA.hello(); }
    }

    // ===================================================================================
    // Configuration
    // ===================================================================================

    @Configuration
    public static class Day11Config {

        @Bean
        @Lazy
        public ExpensivePdfService expensivePdfService() {
            return new ExpensivePdfService();
        }

        @Bean
        public ReportController reportController(ObjectProvider<ExpensivePdfService> pdfServiceProvider) {
            return new ReportController(pdfServiceProvider);
        }

        @Bean
        public AlertService alertService(ObjectProvider<SlackNotifier> slackProvider) {
            return new AlertService(slackProvider);
        }
    }
}
