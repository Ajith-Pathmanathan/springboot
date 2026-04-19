package com.techleadguru.phase7.day140;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class Day140GatewayExceptionHandlingTest {

    @Test
    void testExceptionToStatus400() {
        assertEquals(400,
                Day140GatewayExceptionHandling.exceptionToStatus(
                        new IllegalArgumentException("bad")));
    }

    @Test
    void testExceptionToStatus403() {
        assertEquals(403,
                Day140GatewayExceptionHandling.exceptionToStatus(
                        new Day140GatewayExceptionHandling.AccessDeniedException("forbidden")));
    }

    @Test
    void testExceptionToStatus404() {
        assertEquals(404,
                Day140GatewayExceptionHandling.exceptionToStatus(
                        new NoSuchElementException("not found")));
    }

    @Test
    void testExceptionToStatus500ForUnknown() {
        assertEquals(500,
                Day140GatewayExceptionHandling.exceptionToStatus(
                        new RuntimeException("oops")));
    }

    @Test
    void testBuildErrorResponse() {
        Day140GatewayExceptionHandling.ErrorResponse resp =
                Day140GatewayExceptionHandling.buildErrorResponse(
                        new IllegalArgumentException("invalid id"),
                        "/api/orders",
                        "trace-123");
        assertEquals(400, resp.status());
        assertEquals("/api/orders", resp.path());
        assertEquals("trace-123", resp.traceId());
        assertNotNull(resp.timestamp());
    }

    @Test
    void testHttpReasonPhrases() {
        assertEquals("Bad Request",           Day140GatewayExceptionHandling.httpReasonPhrase(400));
        assertEquals("Unauthorized",          Day140GatewayExceptionHandling.httpReasonPhrase(401));
        assertEquals("Not Found",             Day140GatewayExceptionHandling.httpReasonPhrase(404));
        assertEquals("Too Many Requests",     Day140GatewayExceptionHandling.httpReasonPhrase(429));
        assertEquals("Service Unavailable",   Day140GatewayExceptionHandling.httpReasonPhrase(503));
        assertEquals("Internal Server Error", Day140GatewayExceptionHandling.httpReasonPhrase(999));
    }

    @Test
    void testHandlerGuide() {
        List<Day140GatewayExceptionHandling.ExceptionHandlerGuide> guide =
                Day140GatewayExceptionHandling.handlerGuide();
        assertEquals(5, guide.size());
    }
}
