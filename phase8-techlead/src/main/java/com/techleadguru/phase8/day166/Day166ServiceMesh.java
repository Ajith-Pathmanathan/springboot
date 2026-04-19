package com.techleadguru.phase8.day166;

import java.util.*;

/**
 * Day 166 — Service Mesh vs Spring Cloud
 *
 * Service mesh: infrastructure-level concerns handled by a sidecar proxy
 * (e.g. Istio + Envoy). Language-agnostic.
 *
 * Spring Cloud: library-level concerns baked into the Java application
 * (Gateway, LoadBalancer, Resilience4j, Config Server, etc.).
 *
 * Key question: push cross-cutting concerns into the platform (mesh) or the app?
 */
public class Day166ServiceMesh {

    // ─────────────────────────────────────────────────────────────────────────
    // Service mesh feature record
    // ─────────────────────────────────────────────────────────────────────────

    public record MeshFeature(
            String category,
            String meshApproach,
            String springCloudEquivalent,
            String notes) {}

    public static List<MeshFeature> meshVsSpringCloud() {
        return List.of(
            new MeshFeature("Traffic management",
                "Istio VirtualService / DestinationRule for routing & canary",
                "Spring Cloud Gateway + LoadBalancer",
                "Mesh approach is polyglot; Spring Cloud only for JVM"),
            new MeshFeature("Observability",
                "Envoy sidecar auto-collects metrics, traces, access logs",
                "Spring Actuator + Micrometer + Sleuth/Zipkin",
                "Mesh solution requires no code changes"),
            new MeshFeature("mTLS / Security",
                "Automatic mTLS between sidecars via SPIFFE/SPIRE certificates",
                "Spring Security + manual certificate management",
                "Mesh enforces zero-trust with zero code"),
            new MeshFeature("Circuit breaking",
                "Envoy outlier detection and circuit breaker settings",
                "Resilience4j @CircuitBreaker",
                "Spring Cloud is more fine-grained and code-controlled"),
            new MeshFeature("Retries",
                "Istio retry policy in YAML",
                "Resilience4j @Retry or Spring Retry",
                "Mesh retries are transparent to the application"),
            new MeshFeature("Service discovery",
                "Transparent via DNS + sidecar; no client-side code",
                "Eureka / Consul + DiscoveryClient",
                "Mesh approach simpler operationally"),
            new MeshFeature("Rate limiting",
                "Envoy global rate limiting (sidecar-level)",
                "Spring Cloud Gateway RateLimiter filter",
                "Mesh applies across all languages uniformly")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sidecar proxy model
    // ─────────────────────────────────────────────────────────────────────────

    public record SidecarProxy(
            String podName,
            String proxyType,
            int    proxyPort,
            List<String> capabilities) {}

    public static SidecarProxy exampleSidecar() {
        return new SidecarProxy(
            "order-service-abc123",
            "envoy",
            15001,
            List.of("mTLS termination", "metrics scraping", "distributed tracing",
                    "circuit breaking", "retry", "traffic shifting")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Decision guide
    // ─────────────────────────────────────────────────────────────────────────

    public record DecisionFactor(String factor, String chooseMesh, String chooseSpringCloud) {}

    public static List<DecisionFactor> whenToUseServiceMesh() {
        return List.of(
            new DecisionFactor("Language diversity",
                "Multiple languages/runtimes share cross-cutting concerns",
                "Homogeneous JVM stack — library approach is simpler"),
            new DecisionFactor("Team size",
                "Platform team can own mesh configuration",
                "Small team; fewer operational moving parts preferred"),
            new DecisionFactor("Kubernetes adoption",
                "Already running Kubernetes; mesh is a natural fit",
                "Not on Kubernetes; Spring Cloud libraries are self-contained"),
            new DecisionFactor("Compliance / security",
                "Zero-trust mTLS required without code changes",
                "App-level auth/TLS acceptable"),
            new DecisionFactor("Observability",
                "Need full network-layer telemetry across all services",
                "Micrometer + Actuator covers existing needs")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Istio annotation examples
    // ─────────────────────────────────────────────────────────────────────────

    public static Map<String, String> istioAnnotations() {
        return Map.of(
            "sidecar.istio.io/inject",          "true — enable Envoy sidecar injection",
            "traffic.sidecar.istio.io/includeOutboundIPRanges", "* — intercept all outbound",
            "prometheus.io/scrape",             "true — expose metrics for Prometheus",
            "prometheus.io/port",               "8080 — metrics port",
            "proxy.istio.io/config",            "tracing.sampling: 100 — 100% trace sampling"
        );
    }
}
