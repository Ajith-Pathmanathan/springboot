package com.techleadguru.phase5.day97;

/**
 * DAY 97 — JMH Benchmarks: Measure Before You Optimize
 *
 * JMH (Java Microbenchmark Harness) is the standard tool for micro-benchmarks.
 *   - Handles JIT warmup (runs code until JIT compiles it, then measures)
 *   - Prevents dead-code elimination (uses Blackhole to consume results)
 *   - Reports throughput (ops/s) or average time (ns/op)
 *
 * KEY ANNOTATIONS:
 *   @BenchmarkMode(Mode.AverageTime) — measure average time per op
 *   @OutputTimeUnit(TimeUnit.NANOSECONDS) — output in ns
 *   @State(Scope.Thread) — each benchmark thread has its own instance
 *   @Warmup(iterations = 3, time = 1) — 3 warmup rounds of 1 second
 *   @Measurement(iterations = 5, time = 2) — 5 measurement rounds
 *   @Fork(2) — run in 2 separate JVM processes
 *   Blackhole bh — consume result to prevent dead code elimination
 *
 * RUNNING JMH:
 *   mvn clean package -pl phase5-production -DskipTests
 *   java -jar target/benchmarks.jar  "Day97.*"
 *
 * TYPICAL RESULTS (approximate, varies by JVM + hardware):
 *   String + in loop (n=1000):  ~250 µs/op  (allocates ~500KB per call)
 *   StringBuilder (n=1000):     ~  5 µs/op  (one allocation)
 *   String.join() (n=1000):     ~ 10 µs/op  (one allocation via StringJoiner)
 *
 * LESSON: For single concatenations (2-3 parts), JIT optimizes + to StringBuilder.
 *         In loops, + creates O(n²) work. Always use StringBuilder in loops.
 */
public class Day97StringBenchmark {

    // =========================================================================
    // Non-JMH version — testable with plain JUnit
    // These replicate the benchmark logic without the JMH harness.
    // =========================================================================

    /**
     * BROKEN: String + in a loop — O(n²) allocations.
     * Each + creates a new String object.
     */
    public static String buildWithPlus(int n) {
        String result = "";
        for (int i = 0; i < n; i++) {
            result += "item-" + i + ",";
        }
        return result;
    }

    /**
     * FIXED: StringBuilder — O(n) single buffer, amortized O(1) append.
     */
    public static String buildWithBuilder(int n) {
        StringBuilder sb = new StringBuilder(n * 10); // pre-size hint
        for (int i = 0; i < n; i++) {
            sb.append("item-").append(i).append(',');
        }
        return sb.toString();
    }

    /**
     * FIXED: String.join() — concise, uses StringJoiner under the hood.
     * But requires an array/iterable — less suited for building dynamically.
     */
    public static String buildWithJoin(int n) {
        String[] parts = new String[n];
        for (int i = 0; i < n; i++) {
            parts[i] = "item-" + i;
        }
        return String.join(",", parts);
    }

    /**
     * FIXED: StringJoiner — handles delimiters cleanly.
     */
    public static String buildWithJoiner(int n) {
        java.util.StringJoiner sj = new java.util.StringJoiner(",");
        for (int i = 0; i < n; i++) {
            sj.add("item-" + i);
        }
        return sj.toString();
    }

    // =========================================================================
    // Memory allocation estimate helper
    // =========================================================================

    /**
     * Rough estimate of bytes allocated by buildWithPlus(n).
     * Each iteration creates a new String of increasing length.
     * Sum of 1..n * avg_char_size ≈ n*(n+1)/2 * charWidth bytes
     */
    public static long estimatePlusAllocBytes(int n, int avgCharsPerItem) {
        // Series: 0, avg, 2*avg, ... (n-1)*avg → sum = n*(n-1)/2 * avg
        return (long) n * (n - 1) / 2 * avgCharsPerItem * 2; // UTF-16 = 2 bytes per char
    }

    /**
     * Rough estimate of bytes allocated by buildWithBuilder(n).
     * Single buffer, capacity doubles as needed.
     * Total: ~2 * finalStringSize bytes (one re-size copy at most)
     */
    public static long estimateBuilderAllocBytes(int n, int avgCharsPerItem) {
        long finalSize = (long) n * avgCharsPerItem;
        return finalSize * 2 * 2; // 2x for resize + 2 bytes per char
    }

    // =========================================================================
    // JMH Benchmark class (requires JMH on classpath — test-scoped via pom.xml)
    // This class is compiled but the @Benchmark annotations are only meaningful
    // when run through the JMH harness.
    // =========================================================================

    @SuppressWarnings("unused")
    public static class StringConcatenationBenchmark {

        private static final int N = 1000;

        // No JMH annotations here since JMH is test-scoped — just the logic
        public String plusOperator() {
            return buildWithPlus(N);
        }

        public String stringBuilder() {
            return buildWithBuilder(N);
        }

        public String stringJoiner() {
            return buildWithJoiner(N);
        }
    }
}
