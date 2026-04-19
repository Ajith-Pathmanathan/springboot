package com.techleadguru.phase5.day86;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;

/**
 * DAY 86 — HTTP Client Without Timeout — The Silent Killer
 *
 * SCENARIO:
 *   Your service calls a downstream HTTP API.
 *   The downstream API is slow (network issue, overloaded).
 *   Without timeouts, your thread blocks indefinitely.
 *   With 20+ concurrent slow calls → all threads busy → new requests queue.
 *   Result: your service looks completely down even though it's not your bug.
 *
 * TIMEOUT TYPES:
 *   connectionTimeout — time to establish TCP socket (3–5s typically)
 *   readTimeout       — time to read response bytes after socket connected (5–30s)
 *   writeTimeout      — time to write request body (rare to set separately)
 *   callTimeout       — total time for entire call (connection + request + response)
 *
 * JAVA HTTP CLIENT OPTIONS:
 *   RestClient (Spring 6)  → set via .connectTimeout() + .readTimeout() on builder
 *   RestTemplate           → SimpleClientHttpRequestFactory.setConnectTimeout()
 *   OkHttp                 → OkHttpClient.Builder().callTimeout()
 *   java.net.http.HttpClient → HttpClient.newBuilder().connectTimeout()
 *
 * WITHOUT TIMEOUT — what happens:
 *   Thread blocks at socket read.
 *   Stack trace: sun.nio.ch.SocketChannelImpl.read() (native)
 *   jstack shows: all threads in BLOCKED/WAITING on socket read
 *   Thread pool fills up → new requests get RejectedExecutionException
 *
 * WITH TIMEOUT:
 *   SocketTimeoutException (read) or ConnectTimeoutException after N ms.
 *   Handle with fallback / circuit breaker (Day 144).
 */
@Slf4j
public class Day86HttpClientTimeout {

    // =========================================================================
    // RestClient with proper timeouts (Spring 6 / Spring Boot 3)
    // =========================================================================

    public static class HttpClientConfig {

        /** BROKEN: no timeout — thread can block forever */
        public static RestClient noTimeoutClient() {
            return RestClient.builder().build();
        }

        /** FIXED: explicit connection + read timeouts */
        public static RestClient withTimeoutClient(Duration connectTimeout, Duration readTimeout) {
            var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(connectTimeout);
            factory.setReadTimeout(readTimeout);
            return RestClient.builder()
                    .requestFactory(factory)
                    .build();
        }
    }

    // =========================================================================
    // Java 11 HttpClient with timeout
    // =========================================================================

    public static class JavaHttpClientDemo {

        /** BROKEN: default HttpClient has no timeout by default */
        public static HttpClient withoutTimeout() {
            return HttpClient.newHttpClient();
        }

        /** FIXED: connection timeout enforced at client level */
        public static HttpClient withTimeout(Duration connectTimeout) {
            return HttpClient.newBuilder()
                    .connectTimeout(connectTimeout)
                    .build();
        }

        /**
         * Makes a GET request with per-request timeout.
         * Returns the response body or description of the exception.
         */
        public static String safeGet(HttpClient client, String url, Duration requestTimeout) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(requestTimeout)       // per-request timeout
                    .GET()
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.body();
            } catch (java.net.http.HttpTimeoutException e) {
                return "TIMEOUT: " + e.getMessage();
            } catch (Exception e) {
                return "ERROR: " + e.getMessage();
            }
        }
    }

    // =========================================================================
    // Timeout comparison service
    // =========================================================================

    @Service
    @Slf4j
    public static class DownstreamCallerService {

        private final java.util.concurrent.atomic.AtomicInteger  successCount = new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicInteger  timeoutCount = new java.util.concurrent.atomic.AtomicInteger();

        /**
         * Calls the downstream service with timeout protection.
         * Uses CompletableFuture.orTimeout() to enforce deadline.
         */
        public String callWithTimeout(Callable<String> remoteCall, Duration deadline) {
            try {
                String result = CompletableFuture
                        .supplyAsync(() -> {
                            try { return remoteCall.call(); }
                            catch (Exception e) { throw new CompletionException(e); }
                        })
                        .orTimeout(deadline.toMillis(), TimeUnit.MILLISECONDS)
                        .get();
                successCount.incrementAndGet();
                return result;
            } catch (java.util.concurrent.ExecutionException e) {
                // orTimeout() wraps timeout as CancellationException inside ExecutionException
                timeoutCount.incrementAndGet();
                log.warn("[Day86] Downstream call timed out after {}", deadline);
                return "FALLBACK: downstream unavailable";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "INTERRUPTED";
            }
        }

        public int getSuccessCount() { return successCount.get(); }
        public int getTimeoutCount() { return timeoutCount.get(); }
    }

    // =========================================================================
    // Documentation: what to check when threads are stuck
    // =========================================================================

    public static String stuckThreadDiagnosis() {
        return """
            DIAGNOSIS — threads stuck on HTTP call:
            
            1. jstack <PID> | grep -A 20 "http\\|socket\\|read"
               Look for: sun.nio.ch.SocketChannelImpl.read / java.net.Socket.read
            
            2. Actuator thread dump: GET /actuator/threaddump
               Search for: BLOCKED, WAITING, socket, SocketChannel
            
            3. Actuator metrics:
               GET /actuator/metrics/executor.active
               → if active == max pool size → thread pool full
            
            4. Fix:
               Add connectTimeout + readTimeout to all HTTP clients.
               Wrap slow calls in Circuit Breaker (Resilience4j — Day 144).
            """;
    }
}
