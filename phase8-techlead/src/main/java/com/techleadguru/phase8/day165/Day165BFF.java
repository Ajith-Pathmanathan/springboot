package com.techleadguru.phase8.day165;

import java.util.*;
import java.util.function.Predicate;

/**
 * Day 165 — Backend for Frontend (BFF) Pattern
 *
 * The BFF pattern creates a dedicated backend API per frontend client type.
 * Each BFF aggregates, filters and transforms data tailored to its client.
 *
 * Clients:
 *  WEB        — full data, complex desktop views
 *  MOBILE     — minimal data (bandwidth / battery conscious)
 *  THIRD_PARTY — public API contract, versioned, stable
 */
public class Day165BFF {

    // ─────────────────────────────────────────────────────────────────────────
    // Client types
    // ─────────────────────────────────────────────────────────────────────────

    public enum ClientType { WEB, MOBILE, THIRD_PARTY }

    // ─────────────────────────────────────────────────────────────────────────
    // BFF request / response
    // ─────────────────────────────────────────────────────────────────────────

    public record BffRequest(
            ClientType          clientType,
            String              operation,
            Map<String, String> params) {}

    public record BffResponse(
            Map<String, Object> data,
            Map<String, String> metadata) {

        public static BffResponse of(Map<String, Object> data) {
            return new BffResponse(data, Map.of());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Field-level filter
    // ─────────────────────────────────────────────────────────────────────────

    public static class FieldFilter {

        /** Remove entries from data whose key does NOT match the allowList. */
        public static Map<String, Object> filter(Map<String, Object> data,
                                                  Set<String> allowList) {
            Map<String, Object> result = new LinkedHashMap<>();
            data.forEach((k, v) -> {
                if (allowList.contains(k)) result.put(k, v);
            });
            return result;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Client-specific transformers
    // ─────────────────────────────────────────────────────────────────────────

    public interface BffTransformer {
        ClientType clientType();
        BffResponse transform(Map<String, Object> rawData);
    }

    /** Web BFF — returns all fields with extra display metadata. */
    public static class WebBffTransformer implements BffTransformer {

        @Override public ClientType clientType() { return ClientType.WEB; }

        @Override
        public BffResponse transform(Map<String, Object> rawData) {
            Map<String, Object> enriched = new LinkedHashMap<>(rawData);
            enriched.put("displayMode", "FULL");
            return new BffResponse(enriched, Map.of("version", "web-v1"));
        }
    }

    /** Mobile BFF — strips heavy fields, keeps only essential subset. */
    public static class MobileBffTransformer implements BffTransformer {

        private static final Set<String> MOBILE_FIELDS =
                Set.of("id", "name", "status", "total");

        @Override public ClientType clientType() { return ClientType.MOBILE; }

        @Override
        public BffResponse transform(Map<String, Object> rawData) {
            Map<String, Object> slim = FieldFilter.filter(rawData, MOBILE_FIELDS);
            return new BffResponse(slim, Map.of("version", "mobile-v1", "lite", "true"));
        }
    }

    /** Third-party BFF — stable contract, strips internal fields. */
    public static class ThirdPartyBffTransformer implements BffTransformer {

        private static final Set<String> PUBLIC_FIELDS =
                Set.of("id", "status", "total", "createdAt");

        @Override public ClientType clientType() { return ClientType.THIRD_PARTY; }

        @Override
        public BffResponse transform(Map<String, Object> rawData) {
            Map<String, Object> pub = FieldFilter.filter(rawData, PUBLIC_FIELDS);
            return new BffResponse(pub, Map.of("apiVersion", "1.0", "stable", "true"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BFF gateway
    // ─────────────────────────────────────────────────────────────────────────

    public static class BffGateway {

        private final Map<ClientType, BffTransformer> transformers = new EnumMap<>(ClientType.class);

        public BffGateway register(BffTransformer transformer) {
            transformers.put(transformer.clientType(), transformer);
            return this;
        }

        public BffResponse handle(BffRequest request, Map<String, Object> rawData) {
            BffTransformer t = transformers.get(request.clientType());
            if (t == null)
                throw new IllegalArgumentException("No transformer for: " + request.clientType());
            return t.transform(rawData);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Benefits guide
    // ─────────────────────────────────────────────────────────────────────────

    public record BffBenefit(String benefit, String description) {}

    public static List<BffBenefit> bffBenefits() {
        return List.of(
            new BffBenefit("Client optimisation",
                "Each client gets exactly the data it needs — no over/under-fetching"),
            new BffBenefit("Reduced coupling",
                "Downstream services have no knowledge of client requirements"),
            new BffBenefit("Independent evolution",
                "Mobile and web BFFs can change without affecting each other"),
            new BffBenefit("Security control",
                "Sensitive fields can be stripped per client in one place"),
            new BffBenefit("Aggregation point",
                "BFF aggregates multiple downstream calls into one response")
        );
    }

    public static List<String> aggregationExample() {
        return List.of(
            "1. Client requests /orders/{id} from BFF",
            "2. BFF calls OrderService to get order data",
            "3. BFF calls UserService to enrich with customer info",
            "4. BFF calls InventoryService for stock status",
            "5. BFF calls PricingService for current prices",
            "6. BFF merges and transforms all responses for the client",
            "7. Single response returned to client"
        );
    }
}
