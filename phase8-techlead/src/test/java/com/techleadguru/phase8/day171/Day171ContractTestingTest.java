package com.techleadguru.phase8.day171;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Day171ContractTestingTest {

    private Day171ContractTesting.Contract sampleContract() {
        return new Day171ContractTesting.Contract(
                "consumer-a", "provider-b", "get-product",
                new Day171ContractTesting.ContractRequest("GET", "/api/products/1", null, Map.of()),
                new Day171ContractTesting.ContractResponse(200, "\"id\":1", Map.of())
        );
    }

    @Test
    void contractVerifierPassesWhenStatusAndBodyMatch() {
        var verifier = new Day171ContractTesting.ContractVerifier();
        var result   = verifier.verify(sampleContract(), 200, "{\"id\":1,\"name\":\"Widget\"}");
        assertTrue(result.passed());
        assertNull(result.failureReason());
    }

    @Test
    void contractVerifierFailsOnStatusMismatch() {
        var verifier = new Day171ContractTesting.ContractVerifier();
        var result   = verifier.verify(sampleContract(), 404, "{}");
        assertFalse(result.passed());
        assertNotNull(result.failureReason());
        assertTrue(result.failureReason().contains("Expected status 200"));
    }

    @Test
    void contractVerifierFailsOnBodyMismatch() {
        var verifier = new Day171ContractTesting.ContractVerifier();
        var result   = verifier.verify(sampleContract(), 200, "{}"); // body missing "id":1
        assertFalse(result.passed());
    }

    @Test
    void sampleContractsAreNonEmpty() {
        var contracts = Day171ContractTesting.sampleContracts();
        assertEquals(3, contracts.size());
        contracts.forEach(c -> {
            assertNotNull(c.consumerName());
            assertNotNull(c.providerName());
        });
    }

    @Test
    void contractTestingStepsHaveSevenSteps() {
        var steps = Day171ContractTesting.contractTestingSteps();
        assertEquals(7, steps.size());
    }

    @Test
    void consumerDrivenBenefitsAreNonEmpty() {
        var benefits = Day171ContractTesting.consumerDrivenBenefits();
        assertFalse(benefits.isEmpty());
        benefits.forEach(b -> assertNotNull(b.description()));
    }
}
