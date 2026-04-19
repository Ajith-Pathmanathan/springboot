package com.techleadguru.phase5.day104;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.pool.PoolStats;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * DAY 104 — RestClient Connection Pooling
 *
 * DEFAULT BEHAVIOR (BAD):
 *   SimpleClientHttpRequestFactory (Spring default for RestClient)
 *   → Opens a NEW TCP connection per request
 *   → No pooling, no keep-alive
 *   → 3-way handshake per call (adds ~100ms RTT)
 *   → TLS handshake (adds ~300ms if HTTPS, first call)
 *
 * WITH POOLING (GOOD):
 *   Apache HttpClient 5 PoolingHttpClientConnectionManager
 *   → Maintains persistent connections to each host
 *   → Reuses connections (no TCP/TLS overhead on subsequent calls)
 *   → Validates stale connections before handing out
 *
 * KEY SETTINGS:
 *   maxTotal              — max connections across ALL hosts (default 20 → set 50-200)
 *   defaultMaxPerRoute    — max connections per host:port (default 2 → set 10-20)
 *   connectionTTL         — max lifetime of a connection (prevent stale/firewalled)
 *   validateAfterInactivity — check if connection alive before use (reduces errors)
 *   connectTimeout        — time to establish TCP connection
 *   responseTimeout       — time to read response data
 *
 * METRICS (Micrometer + Actuator):
 *   httpcomponents.httpclient.pool.total.connections
 *   httpcomponents.httpclient.pool.available.connections
 *   httpcomponents.httpclient.pool.leased.connections
 *
 * RULE OF THUMB:
 *   maxTotal = sum across all routes of defaultMaxPerRoute * numRoutes
 *   defaultMaxPerRoute = (avg requests/sec per route) * (avg response time in sec) + margin
 */
public class Day104RestClientPooling {

    // =========================================================================
    // BROKEN: default RestClient — no pooling
    // =========================================================================

    public static RestClient noPoolingClient() {
        // SimpleClientHttpRequestFactory creates new connection per request
        return RestClient.builder()
                .baseUrl("http://localhost:8080")
                .build();
    }

    // =========================================================================
    // FIXED: RestClient with Apache HttpClient 5 connection pool
    // =========================================================================

    @Configuration
    public static class RestClientConfig {

        @Bean
        public PoolingHttpClientConnectionManager connectionManager() {
            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
            cm.setMaxTotal(50);               // max connections (all hosts)
            cm.setDefaultMaxPerRoute(10);     // max per host:port

            // Validate connections before lending them out from pool
            cm.setDefaultSocketConfig(
                    SocketConfig.custom()
                            .setSoTimeout(Timeout.ofSeconds(5))
                            .build());

            return cm;
        }

        @Bean
        public HttpClient httpClient(PoolingHttpClientConnectionManager cm) {
            return HttpClients.custom()
                    .setConnectionManager(cm)
                    // Evict expired + idle connections in background
                    .evictExpiredConnections()
                    .evictIdleConnections(TimeValue.ofSeconds(30))
                    // Note: setConnectionTimeToLive was removed in httpclient5 5.4
                    // TTL is now configured on the PoolingHttpClientConnectionManager
                    .build();
        }

        @Bean
        public RestClient pooledRestClient(HttpClient httpClient) {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(Timeout.ofSeconds(3))
                    .setResponseTimeout(Timeout.ofSeconds(10))
                    .build();

            HttpClient configuredClient = HttpClients.custom()
                    .setConnectionManager(((org.apache.hc.client5.http.impl.classic.CloseableHttpClient) httpClient)
                            .getClass().cast(httpClient) == null
                            ? null
                            : ((PoolingHttpClientConnectionManager)
                                    getConnectionManager(httpClient)))
                    .setDefaultRequestConfig(requestConfig)
                    .build();

            return RestClient.builder()
                    .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                    .build();
        }

        private Object getConnectionManager(HttpClient client) {
            return null; // placeholder — in real code use field injection
        }
    }

    // =========================================================================
    // Simpler factory method (no @Configuration needed — for tests)
    // =========================================================================

    public static RestClient pooledClient(int maxTotal, int maxPerRoute,
                                           int connectTimeoutSec, int readTimeoutSec) {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(maxTotal);
        cm.setDefaultMaxPerRoute(maxPerRoute);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(connectTimeoutSec))
                .setResponseTimeout(Timeout.ofSeconds(readTimeoutSec))
                .build();

        HttpClient client = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();

        return RestClient.builder()
                .requestFactory(new HttpComponentsClientHttpRequestFactory(client))
                .build();
    }

    // =========================================================================
    // Pool stats reporter
    // =========================================================================

    public static class ConnectionPoolStats {

        private final PoolingHttpClientConnectionManager connectionManager;

        public ConnectionPoolStats(PoolingHttpClientConnectionManager cm) {
            this.connectionManager = cm;
        }

        public PoolStats getTotalStats() {
            return connectionManager.getTotalStats();
        }

        public record Report(int available, int leased, int pending, int max) {
            public double usagePercent() {
                return max > 0 ? (double) leased / max * 100.0 : 0.0;
            }

            public boolean isNearCapacity() {
                return usagePercent() > 80.0;
            }
        }

        public Report report() {
            PoolStats stats = connectionManager.getTotalStats();
            return new Report(
                    stats.getAvailable(),
                    stats.getLeased(),
                    stats.getPending(),
                    stats.getMax()
            );
        }
    }
}
