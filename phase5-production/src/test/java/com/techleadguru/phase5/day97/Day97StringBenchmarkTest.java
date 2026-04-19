package com.techleadguru.phase5.day97;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class Day97StringBenchmarkTest {

    @Test
    void buildWithPlus_and_buildWithBuilder_produce_same_result() {
        String plus    = Day97StringBenchmark.buildWithPlus(50);
        String builder = Day97StringBenchmark.buildWithBuilder(50);
        assertThat(plus).isEqualTo(builder);
    }

    @Test
    void buildWithJoin_and_buildWithJoiner_produce_same_result() {
        String join   = Day97StringBenchmark.buildWithJoin(50);
        String joiner = Day97StringBenchmark.buildWithJoiner(50);
        assertThat(join).isEqualTo(joiner);
    }

    @Test
    void buildWithJoiner_produces_comma_separated_items() {
        String result = Day97StringBenchmark.buildWithJoiner(3);
        assertThat(result).isEqualTo("item-0,item-1,item-2");
    }

    @Test
    void buildWithBuilder_completes_in_reasonable_time_for_large_n() {
        // StringBuilder should handle 10000 items quickly
        long start = System.currentTimeMillis();
        Day97StringBenchmark.buildWithBuilder(10_000);
        long elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed).isLessThan(1000); // well under 1 second
    }

    @Test
    void estimatePlusAllocBytes_grows_with_n_squared() {
        long small = Day97StringBenchmark.estimatePlusAllocBytes(10, 10);
        long large = Day97StringBenchmark.estimatePlusAllocBytes(100, 10);
        // 100x n → 10000x allocations (quadratic), large should be >> small * 10
        assertThat(large).isGreaterThan(small * 50);
    }

    @Test
    void estimateBuilderAllocBytes_grows_linearly_with_n() {
        long small = Day97StringBenchmark.estimateBuilderAllocBytes(10, 10);
        long large = Day97StringBenchmark.estimateBuilderAllocBytes(100, 10);
        // Linear growth: 10x n → ~10x bytes
        assertThat(large).isCloseTo(small * 10, withinPercentage(50));
    }

    @Test
    void stringConcatenationBenchmark_methods_produce_non_null_results() {
        var benchmark = new Day97StringBenchmark.StringConcatenationBenchmark();
        assertThat(benchmark.plusOperator()).isNotNull();
        assertThat(benchmark.stringBuilder()).isNotNull();
        assertThat(benchmark.stringJoiner()).isNotNull();
    }
}
