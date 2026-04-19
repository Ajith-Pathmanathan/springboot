package com.techleadguru.phase1.day12;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * DAY 12 — Optional Dependencies: Optional<T> Injection
 *
 * PROBLEM:
 *   A service optionally uses a third-party integration (e.g., Slack, Stripe, PagerDuty).
 *   In dev/test profiles, the integration bean doesn't exist.
 *   @Autowired throws NoSuchBeanDefinitionException at startup.
 *   @Autowired(required=false) works but gives null — risk of NPE in production.
 *
 * SOLUTION: Inject Optional<T>
 *   Spring 4.3+ supports @Autowired Optional<T>.
 *   If no matching bean: Optional.empty() is injected. No exception. No null.
 *   If bean exists: Optional.of(bean) is injected.
 *   Code uses optional.ifPresent() or optional.map() — null-safe by design.
 *
 * COMPARISON:
 *   @Autowired(required=false)  → field/param is null if missing — NPE risk
 *   Optional<T>                 → empty Optional if missing — safe
 *   ObjectProvider<T>           → lazy, stream-capable, prototype-aware — most powerful
 *
 * PRODUCTION SCENARIO — NPE in production Slack integration:
 *   @Autowired(required=false) SlackClient slackClient;
 *   Dev environment: slackClient is null. No bug. Tests pass.
 *   Staging: SlackClient bean exists. Works fine.
 *   Production: SlackClient misconfigured on 3 out of 10 pods.
 *   Those 3 pods: NPE whenever alert is sent. Silently swallowed by @ControllerAdvice.
 *   FIX: Optional<SlackClient>. All paths handle absent bean explicitly.
 */
@Slf4j
public class Day12OptionalInjection {

    // ===================================================================================
    // Optional integration dependency
    // ===================================================================================

    public interface AlertChannel {
        void send(String alert);
    }

    public static class SlackAlertChannel implements AlertChannel {
        @Override
        public void send(String alert) {
            System.out.println("[Slack] " + alert);
        }
    }

    // ===================================================================================
    // Service using Optional<T> — safe regardless of whether bean exists
    // ===================================================================================

    @Slf4j
    public static class MonitoringService {

        private final Optional<AlertChannel> alertChannel; // empty if no bean registered

        public MonitoringService(Optional<AlertChannel> alertChannel) {
            this.alertChannel = alertChannel;
            log.info("[Day12] MonitoringService created. Slack present: {}", alertChannel.isPresent());
        }

        public void checkSystem(String metric, double value) {
            if (value > 90.0) {
                String alert = "ALERT: " + metric + " = " + value + "%";
                // null-safe: no NPE possible
                alertChannel.ifPresent(channel -> channel.send(alert));
                if (alertChannel.isEmpty()) {
                    log.warn("[Day12] No alert channel configured. Alert: {}", alert);
                }
            }
        }

        public boolean hasAlertChannel() {
            return alertChannel.isPresent();
        }

        public String describeChannel() {
            return alertChannel.map(c -> c.getClass().getSimpleName()).orElse("NONE");
        }
    }

    // ===================================================================================
    // Configuration — profile-dependent bean registration
    // ===================================================================================

    @Configuration
    public static class Day12Config {

        @Bean
        public SlackAlertChannel slackAlertChannel() {
            return new SlackAlertChannel();
        }

        @Bean
        public MonitoringService monitoringServiceWithSlack(Optional<AlertChannel> alertChannel) {
            return new MonitoringService(alertChannel);
        }
    }
}
