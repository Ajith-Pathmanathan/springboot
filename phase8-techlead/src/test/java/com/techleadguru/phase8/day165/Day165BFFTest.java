package com.techleadguru.phase8.day165;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Day165BFFTest {

    private Map<String, Object> sampleRaw() {
        return Map.of(
                "id",         "o1",
                "name",       "Widget Order",
                "status",     "PENDING",
                "total",      99.0,
                "createdAt",  "2024-01-01",
                "internalRef","INT-001"
        );
    }

    @Test
    void webBffReturnsAllFieldsPlusDisplayMode() {
        var t = new Day165BFF.WebBffTransformer();
        var response = t.transform(sampleRaw());

        assertEquals(Day165BFF.ClientType.WEB, t.clientType());
        assertEquals("FULL", response.data().get("displayMode"));
        assertTrue(response.data().containsKey("internalRef")); // all fields kept
    }

    @Test
    void mobileBffFiltersToEssentialFields() {
        var t = new Day165BFF.MobileBffTransformer();
        var response = t.transform(sampleRaw());

        assertEquals(Day165BFF.ClientType.MOBILE, t.clientType());
        assertTrue(response.data().containsKey("id"));
        assertTrue(response.data().containsKey("status"));
        assertFalse(response.data().containsKey("internalRef")); // stripped
        assertEquals("mobile-v1", response.metadata().get("version"));
    }

    @Test
    void thirdPartyBffExposesOnlyPublicFields() {
        var t = new Day165BFF.ThirdPartyBffTransformer();
        var response = t.transform(sampleRaw());

        assertEquals(Day165BFF.ClientType.THIRD_PARTY, t.clientType());
        assertTrue(response.data().containsKey("id"));
        assertFalse(response.data().containsKey("name"));
        assertFalse(response.data().containsKey("internalRef"));
        assertEquals("1.0", response.metadata().get("apiVersion"));
    }

    @Test
    void bffGatewayRoutesToCorrectTransformer() {
        var gateway = new Day165BFF.BffGateway()
                .register(new Day165BFF.WebBffTransformer())
                .register(new Day165BFF.MobileBffTransformer())
                .register(new Day165BFF.ThirdPartyBffTransformer());

        var request = new Day165BFF.BffRequest(Day165BFF.ClientType.MOBILE, "getOrder", Map.of());
        var response = gateway.handle(request, sampleRaw());

        assertFalse(response.data().containsKey("internalRef")); // mobile filter applied
    }

    @Test
    void bffBenefitsAreNonEmpty() {
        assertFalse(Day165BFF.bffBenefits().isEmpty());
        assertFalse(Day165BFF.aggregationExample().isEmpty());
    }
}
