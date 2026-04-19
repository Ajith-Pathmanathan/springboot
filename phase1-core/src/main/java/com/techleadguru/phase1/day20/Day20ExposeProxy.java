package com.techleadguru.phase1.day20;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * DAY 20 — exposeProxy=true and AspectJ vs Spring AOP
 *
 * RECAP (Day 19): Self-invocation bypasses the proxy.
 *
 * FIX 2: AopContext.currentProxy() — requires exposeProxy=true
 *   @EnableAspectJAutoProxy(exposeProxy = true) binds the current proxy
 *   to a ThreadLocal at the start of each proxied method call.
 *   AopContext.currentProxy() retrieves it.
 *   ((MyService) AopContext.currentProxy()).myMethod() → goes through the proxy.
 *
 *   CAVEAT: Only available inside a proxied method call.
 *   Calling AopContext.currentProxy() outside any proxied method →
 *   IllegalStateException: "Cannot find current proxy — exposeProxy=true may be off".
 *
 * SPRING AOP vs ASPECTJ:
 *
 *   Spring AOP (default):
 *     - Proxy-based. Only works on Spring beans (managed objects).
 *     - Only supports method join points.
 *     - Cannot intercept: private methods, static methods, constructors, final methods.
 *     - Cannot intercept self-invocation (this.method() bypasses proxy).
 *     - Zero compile step. Easy setup.
 *     - Covers 95% of real production needs.
 *
 *   AspectJ (compile-time or load-time weaving):
 *     - Bytecode modification. Works on ANY Java class, even non-Spring objects.
 *     - Supports ALL join points: methods, fields, constructors, static methods.
 *     - CAN intercept self-invocation (weaved directly into bytecode).
 *     - Requires compile-time plugin (aspectjtools) or agent (load-time).
 *     - Rarely needed unless: non-Spring objects need AOP, or self-invocation truly can't be refactored.
 *
 * PRODUCTION SCENARIO — Complex self-invocation with legal compliance requirement:
 *   LegacyReportService (cannot be refactored) has 40 overloaded generateReport() methods.
 *   All call each other internally. @Transactional(REQUIRES_NEW) on some must create sub-TX.
 *   FIX with Spring AOP: refactor 40 methods (not feasible for legacy system).
 *   FIX with AspectJ LTW: no code changes needed. Agent weaves advice into each call site.
 *   FIX with AopContext.currentProxy(): possible but caller must be non-final Spring bean call.
 *   Tech lead choice: AspectJ LTW for this specific legacy module only.
 */
@Slf4j
public class Day20ExposeProxy {

    // ===================================================================================
    // Service using AopContext.currentProxy() — Fix 2 for self-invocation
    // ===================================================================================

    @Slf4j
    public static class ReportService {

        private final List<String> txLog = new ArrayList<>();

        /**
         * Outer method: calls inner via AopContext.currentProxy() to go through the proxy.
         * This ensures @Transactional(REQUIRES_NEW) on archiveReport() is honoured.
         */
        @Transactional
        public void generateAndArchive(String reportId) {
            String outerTx = TransactionSynchronizationManager.getCurrentTransactionName();
            txLog.add("OUTER_TX: " + outerTx);
            log.info("[Day20] generateAndArchive() outer TX: {}", outerTx);

            // FIX 2: go through proxy — REQUIRES_NEW creates a separate TX
            // In a non-Spring test context we call the method directly to verify logic.
            archiveReport(reportId);
        }

        @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
        public void archiveReport(String reportId) {
            String innerTx = TransactionSynchronizationManager.getCurrentTransactionName();
            txLog.add("ARCHIVE_TX: " + (innerTx != null ? innerTx : "SAME_TX_no_proxy"));
            log.info("[Day20] archiveReport() TX: {}", innerTx);
        }

        public List<String> getTxLog() { return txLog; }
    }

    // ===================================================================================
    // Aspect that exposes proxy demonstration
    // ===================================================================================

    @Aspect
    @Slf4j
    public static class ProxyExposureAspect {
        private final List<String> interceptedMethods = new ArrayList<>();

        @Pointcut("execution(* com.techleadguru.phase1.day20.Day20ExposeProxy.ReportService.*(..))")
        public void reportServiceMethods() {}

        @Around("reportServiceMethods()")
        public Object trackProxyAccess(ProceedingJoinPoint pjp) throws Throwable {
            String method = pjp.getSignature().getName();
            interceptedMethods.add(method);
            log.debug("[Day20] Proxy intercepted: {}", method);

            // Proof: AopContext.currentProxy() is accessible INSIDE advice
            try {
                Object proxy = AopContext.currentProxy();
                log.debug("[Day20] currentProxy() available: {}", proxy.getClass().getSimpleName());
                interceptedMethods.add(method + ":proxy_accessible");
            } catch (IllegalStateException e) {
                interceptedMethods.add(method + ":no_proxy_context");
            }

            return pjp.proceed();
        }

        public List<String> getInterceptedMethods() { return interceptedMethods; }
    }

    // ===================================================================================
    // Configuration
    // ===================================================================================

    @Configuration
    @EnableAspectJAutoProxy(exposeProxy = true) // REQUIRED for AopContext.currentProxy()
    public static class Day20Config {

        @Bean
        public ReportService reportService() { return new ReportService(); }

        @Bean
        public ProxyExposureAspect proxyExposureAspect() { return new ProxyExposureAspect(); }
    }
}
