package com.techleadguru.phase8.day172;

import java.util.*;

/**
 * Day 172 — Load Testing with k6
 *
 * k6 is a developer-centric, script-based load testing tool.
 * Tests are written in JavaScript and run as Go-native executors.
 *
 * Test types:
 *  Smoke    — 1-2 VUs, verify the baseline is stable
 *  Load     — expected peak traffic, validate SLA
 *  Stress   — increase load until system degrades
 *  Spike    — sudden traffic burst, recovery testing
 *  Soak     — sustained load over hours (memory leaks, resource exhaustion)
 */
public class Day172LoadTesting {

    // ─────────────────────────────────────────────────────────────────────────
    // Load test scenario
    // ─────────────────────────────────────────────────────────────────────────

    public record LoadTestScenario(
            String       name,
            int          vus,
            String       duration,
            Map<String, String> thresholds) {}

    public static LoadTestScenario smokeTest() {
        return new LoadTestScenario("smoke", 1, "30s",
                Map.of("http_req_failed", "rate<0.01",
                       "http_req_duration", "p(95)<500"));
    }

    public static LoadTestScenario loadTest() {
        return new LoadTestScenario("load", 100, "5m",
                Map.of("http_req_failed", "rate<0.01",
                       "http_req_duration", "p(95)<1000",
                       "http_req_duration", "p(99)<2000"));
    }

    public static LoadTestScenario stressTest() {
        return new LoadTestScenario("stress", 500, "10m",
                Map.of("http_req_failed", "rate<0.05",
                       "http_req_duration", "p(95)<3000"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load test result
    // ─────────────────────────────────────────────────────────────────────────

    public record LoadTestResult(
            long   requestsTotal,
            double failureRate,
            long   p95LatencyMs,
            long   p99LatencyMs,
            double rps) {

        public boolean meetsThreshold(long maxP95Ms, double maxFailureRate) {
            return p95LatencyMs <= maxP95Ms && failureRate <= maxFailureRate;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Threshold evaluator
    // ─────────────────────────────────────────────────────────────────────────

    public record ThresholdSpec(String metric, String condition, String threshold) {}

    public static class ThresholdEvaluator {

        public boolean evaluate(LoadTestResult result, List<ThresholdSpec> thresholds) {
            for (ThresholdSpec t : thresholds) {
                if (!passes(result, t)) return false;
            }
            return true;
        }

        private boolean passes(LoadTestResult r, ThresholdSpec t) {
            return switch (t.metric()) {
                case "failure_rate"  -> r.failureRate()  <= parseDouble(t.threshold());
                case "p95_latency"   -> r.p95LatencyMs() <= parseLong(t.threshold());
                case "p99_latency"   -> r.p99LatencyMs() <= parseLong(t.threshold());
                case "rps"           -> r.rps()          >= parseDouble(t.threshold());
                default              -> true;
            };
        }

        private static double parseDouble(String s) { return Double.parseDouble(s); }
        private static long   parseLong(String s)   { return Long.parseLong(s); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // k6 script example (as a string for reference)
    // ─────────────────────────────────────────────────────────────────────────

    public static String k6ScriptExample() {
        return """
            import http from 'k6/http';
            import { check, sleep } from 'k6';

            export const options = {
              stages: [
                { duration: '1m', target: 50 },   // ramp up
                { duration: '3m', target: 50 },   // steady state
                { duration: '1m', target: 0  },   // ramp down
              ],
              thresholds: {
                http_req_failed:   ['rate<0.01'],
                http_req_duration: ['p(95)<1000'],
              },
            };

            export default function () {
              const res = http.get('http://localhost:8080/api/orders');
              check(res, {
                'status is 200':      (r) => r.status === 200,
                'response time < 1s': (r) => r.timings.duration < 1000,
              });
              sleep(1);
            }
            """;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load test types guide
    // ─────────────────────────────────────────────────────────────────────────

    public record LoadTestType(
            String type,
            String vus,
            String duration,
            String goal) {}

    public static List<LoadTestType> loadTestTypes() {
        return List.of(
            new LoadTestType("Smoke",   "1-2",    "30s-2m",   "Verify baseline; sanity check"),
            new LoadTestType("Load",    "~100",   "5-30m",    "Validate SLA at expected peak"),
            new LoadTestType("Stress",  "500+",   "30-60m",   "Find breaking point"),
            new LoadTestType("Spike",   "0→1000 sudden", "seconds to minutes", "Simulate viral traffic burst"),
            new LoadTestType("Soak",    "moderate", "hours",  "Detect memory leaks and resource exhaustion")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Interpret results guide
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> interpretResults() {
        return List.of(
            "http_req_duration p(95) > SLA? → Latency problem: profile with async profiler",
            "http_req_failed rate > 1%? → Error rate too high: check error logs, circuit breaker",
            "RPS plateaus under load? → Likely thread pool saturation or DB connection pool exhausted",
            "GC pause spikes under load? → Heap too small; tune -Xmx or switch to G1/ZGC",
            "CPU > 80% sustained? → Scale out horizontally or profile hot methods",
            "Memory grows linearly? → Memory leak; use heap dump + MAT to find retention",
            "Latency increases over time (soak)? → Connection pool leak or growing cache"
        );
    }
}
