package com.techleadguru.phase5.day104;

import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class Day104RestClientPoolingTest {

    @Test
    void pooledClient_created_successfully() {
        var client = Day104RestClientPooling.pooledClient(20, 5, 3, 10);
        assertThat(client).isNotNull();
    }

    @Test
    void connectionPoolStats_report_initial_values() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(50);
        cm.setDefaultMaxPerRoute(10);

        var stats = new Day104RestClientPooling.ConnectionPoolStats(cm);
        var report = stats.report();

        assertThat(report.available()).isGreaterThanOrEqualTo(0);
        assertThat(report.leased()).isEqualTo(0);
        assertThat(report.pending()).isEqualTo(0);
        assertThat(report.max()).isEqualTo(50);
        cm.close();
    }

    @Test
    void report_usagePercent_is_zero_when_no_connections_leased() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);
        var stats = new Day104RestClientPooling.ConnectionPoolStats(cm);
        assertThat(stats.report().usagePercent()).isZero();
        cm.close();
    }

    @Test
    void report_isNearCapacity_false_when_usage_below_80() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);
        var stats = new Day104RestClientPooling.ConnectionPoolStats(cm);
        assertThat(stats.report().isNearCapacity()).isFalse();
        cm.close();
    }

    @Test
    void report_isNearCapacity_manual_check_above_80_percent() {
        // Verify the math: 90% leased → isNearCapacity should be true
        var report = new Day104RestClientPooling.ConnectionPoolStats.Report(0, 90, 0, 100);
        assertThat(report.usagePercent()).isEqualTo(90.0);
        assertThat(report.isNearCapacity()).isTrue();
    }

    @Test
    void report_isNearCapacity_false_at_exactly_80_percent() {
        var report = new Day104RestClientPooling.ConnectionPoolStats.Report(0, 80, 0, 100);
        assertThat(report.isNearCapacity()).isFalse(); // > 80, not >=
    }

    @Test
    void noPoolingClient_created_successfully() {
        var client = Day104RestClientPooling.noPoolingClient();
        assertThat(client).isNotNull();
    }
}
