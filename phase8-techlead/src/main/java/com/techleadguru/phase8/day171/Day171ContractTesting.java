package com.techleadguru.phase8.day171;

import java.util.*;

/**
 * Day 171 — Contract Testing: Spring Cloud Contract
 *
 * Consumer-driven contract testing ensures that a provider (server)
 * satisfies the expectations defined by a consumer (client).
 *
 * Flow:
 *  1. Consumer writes a contract (Groovy DSL or YAML)
 *  2. Spring Cloud Contract Verifier generates provider test stubs
 *  3. Provider runs auto-generated tests to verify the contract
 *  4. Stubs are published to a repository (Maven / Pact Broker)
 *  5. Consumer uses stubs in its own tests (no real server needed)
 */
public class Day171ContractTesting {

    // ─────────────────────────────────────────────────────────────────────────
    // Contract model
    // ─────────────────────────────────────────────────────────────────────────

    public record ContractRequest(
            String              method,
            String              url,
            String              body,       // nullable
            Map<String, String> headers) {}

    public record ContractResponse(
            int                 status,
            String              body,       // nullable
            Map<String, String> headers) {}

    public record Contract(
            String           consumerName,
            String           providerName,
            String           contractName,
            ContractRequest  request,
            ContractResponse response) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Contract verification result
    // ─────────────────────────────────────────────────────────────────────────

    public record ContractVerification(
            String  contractName,
            boolean passed,
            String  failureReason) {

        public static ContractVerification success(String contractName) {
            return new ContractVerification(contractName, true, null);
        }

        public static ContractVerification failure(String contractName, String reason) {
            return new ContractVerification(contractName, false, reason);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Simple contract verifier (in-memory simulation)
    // ─────────────────────────────────────────────────────────────────────────

    public static class ContractVerifier {

        /** Verify an actual HTTP response against the contract requirements. */
        public ContractVerification verify(Contract contract,
                                           int      actualStatus,
                                           String   actualBody) {
            int expectedStatus = contract.response().status();
            if (actualStatus != expectedStatus) {
                return ContractVerification.failure(contract.contractName(),
                        "Expected status " + expectedStatus + " but got " + actualStatus);
            }
            String expectedBody = contract.response().body();
            if (expectedBody != null && !actualBody.contains(expectedBody)) {
                return ContractVerification.failure(contract.contractName(),
                        "Response body does not match contract expectation");
            }
            return ContractVerification.success(contract.contractName());
        }

        public List<ContractVerification> verifyAll(
                List<Contract> contracts,
                java.util.function.BiFunction<String, String, int[]> callProvider) {
            List<ContractVerification> results = new ArrayList<>();
            for (Contract c : contracts) {
                int[] statusAndBody = callProvider.apply(c.request().method(), c.request().url());
                // statusAndBody[0] = status; actual body derived separately
                results.add(verify(c, statusAndBody[0], ""));
            }
            return results;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sample contracts guide
    // ─────────────────────────────────────────────────────────────────────────

    public static List<Contract> sampleContracts() {
        return List.of(
            new Contract(
                "order-service-consumer",
                "product-service",
                "get-product-by-id",
                new ContractRequest("GET", "/api/products/1", null,
                        Map.of("Accept", "application/json")),
                new ContractResponse(200, "{\"id\":1,\"name\":\"Widget\"}",
                        Map.of("Content-Type", "application/json"))),
            new Contract(
                "checkout-service",
                "inventory-service",
                "check-stock",
                new ContractRequest("GET", "/api/inventory/sku123/stock", null, Map.of()),
                new ContractResponse(200, "{\"sku\":\"sku123\",\"quantity\":50}", Map.of())),
            new Contract(
                "notification-service",
                "order-service",
                "order-status-event",
                new ContractRequest("POST", "/api/orders/events", "{\"status\":\"CONFIRMED\"}", Map.of()),
                new ContractResponse(202, null, Map.of()))
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Testing steps guide
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> contractTestingSteps() {
        return List.of(
            "1. Consumer writes contract in src/test/resources/contracts/ (Groovy DSL or YAML)",
            "2. Run mvn generate-test-sources on provider — SCC generates JUnit tests",
            "3. Provider extends auto-generated base class and provides real/mock beans",
            "4. Run generated tests — they call real API and assert against contract",
            "5. On pass, stubs packaged as JAR: <artifactId>-stubs.jar",
            "6. Consumer loads stub via @AutoConfigureStubRunner(ids=\"group:artifact:+:stubs:PORT\")",
            "7. Consumer tests run against stub — no real provider needed"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Consumer-driven benefits
    // ─────────────────────────────────────────────────────────────────────────

    public record ConsumerDrivenBenefit(String benefit, String description) {}

    public static List<ConsumerDrivenBenefit> consumerDrivenBenefits() {
        return List.of(
            new ConsumerDrivenBenefit("Provider safety",
                "Provider knows exactly what consumers rely on — safe to refactor"),
            new ConsumerDrivenBenefit("Independent deployment",
                "Deploy consumer and provider independently without integration tests on every deploy"),
            new ConsumerDrivenBenefit("Fast feedback",
                "Stub-based consumer tests run without starting provider service"),
            new ConsumerDrivenBenefit("Living documentation",
                "Contracts are executable specifications of API behaviour"),
            new ConsumerDrivenBenefit("Breaking change detection",
                "Provider CI fails if a contract is broken before reaching consumers")
        );
    }
}
