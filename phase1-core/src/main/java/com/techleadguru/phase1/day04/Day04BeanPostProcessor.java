package com.techleadguru.phase1.day04;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * DAY 4 — BeanPostProcessor: Intercept EVERY bean at init time.
 *
 * BeanPostProcessor has two hooks:
 *   postProcessBeforeInitialization(bean, name) — fires BEFORE @PostConstruct / afterPropertiesSet
 *   postProcessAfterInitialization(bean, name)  — fires AFTER all init methods
 *
 * This is HOW Spring itself implements:
 *   - @Autowired injection         (AutowiredAnnotationBeanPostProcessor)
 *   - @PostConstruct / @PreDestroy (CommonAnnotationBeanPostProcessor)
 *   - AOP proxy creation           (AbstractAutoProxyCreator)
 *   - @Value injection             (AutowiredAnnotationBeanPostProcessor)
 *   - @Async / @Transactional proxy wrapping
 *
 * PRODUCTION SCENARIO — Slow BPP causes K8s restart loop:
 *   BeanPostProcessor calls a feature-flag API HTTP call for EVERY bean.
 *   300+ beans x 50ms per call = 15+ seconds extra startup time.
 *   K8s liveness probe times out (default 30s), kills pod. CrashLoopBackOff.
 *   FIX: Cache the API response. Fetch once in @PostConstruct, use cached value in BPP.
 *
 * HOW TO RUN:
 *   Start Phase1Application. Every bean name is printed by StartupTimingBeanPostProcessor.
 */
@Slf4j
public class Day04BeanPostProcessor {

    // ------------------------------------------------------------------------------------
    // BPP #1: Logs every bean name as it initialises — shows ALL Spring internal beans too
    // ------------------------------------------------------------------------------------

    @Component
    public static class BeanInventoryLogger implements BeanPostProcessor {

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            log.debug("[BPP BEFORE] '{}' ({})", beanName, bean.getClass().getSimpleName());
            return bean; // ALWAYS return bean — returning null breaks the context
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            log.debug("[BPP AFTER]  '{}' ready", beanName);
            return bean; // ALWAYS return bean or a wrapper proxy
        }
    }

    // ------------------------------------------------------------------------------------
    // BPP #2: ExecutionTimeTracker — measures init time per bean
    // Simulates the production scenario: slow BPP slows down startup
    // ------------------------------------------------------------------------------------

    @Component
    public static class StartupTimingBeanPostProcessor implements BeanPostProcessor {

        private final ThreadLocal<Long> startTime = new ThreadLocal<>();

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            startTime.set(System.nanoTime());
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            long elapsed = System.nanoTime() - startTime.get();
            startTime.remove(); // CRITICAL: always remove ThreadLocal in thread pool / BPP

            if (elapsed > 5_000_000L) { // warn if init > 5ms
                log.warn("[SLOW BEAN INIT] '{}' took {}ms — investigate if this is a BPP doing I/O",
                        beanName, elapsed / 1_000_000L);
            }
            return bean;
        }
    }

    // ------------------------------------------------------------------------------------
    // BPP #3: Production pattern — validate @Service beans have required annotations
    // Catches missing @Transactional at startup rather than at runtime
    // ------------------------------------------------------------------------------------

    @Component
    public static class ServiceAnnotationValidator implements BeanPostProcessor {

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            boolean isService = bean.getClass().isAnnotationPresent(org.springframework.stereotype.Service.class);
            if (isService) {
                log.info("[BPP VALIDATOR] @Service bean '{}' registered and validated", beanName);
                // In real production: check if service has @Transactional, log warning if not
            }
            return bean;
        }
    }

    // ------------------------------------------------------------------------------------
    // Demo service that goes through the BPPs above
    // ------------------------------------------------------------------------------------

    @Configuration
    static class Day04Config {
        @Bean
        public SlowDemoService slowDemoService() {
            return new SlowDemoService();
        }
    }

    public static class SlowDemoService {
        // Has a slightly slow init to trigger the timing BPP warning
        @jakarta.annotation.PostConstruct
        public void init() throws InterruptedException {
            Thread.sleep(10); // simulate 10ms init (triggers BPP timing warning)
        }
    }
}
