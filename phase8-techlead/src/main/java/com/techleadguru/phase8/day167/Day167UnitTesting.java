package com.techleadguru.phase8.day167;

import java.util.*;

/**
 * Day 167 — Unit Testing: Mock vs Spy, Argument Captors
 *
 * Key Mockito patterns:
 *  - Mock:        fully fake object; all methods no-op/null unless stubbed
 *  - Spy:         partial mock; wraps a real object, real methods unless stubbed
 *  - Captor:      captures arguments passed to a mock method for assertion
 *  - InOrder:     verify calls in specific order
 *  - ArgumentMatcher: flexible argument matching
 */
public class Day167UnitTesting {

    // ─────────────────────────────────────────────────────────────────────────
    // Test double taxonomy
    // ─────────────────────────────────────────────────────────────────────────

    public enum TestDoubleType {
        DUMMY,  // passed but never used
        STUB,   // provides canned answers
        FAKE,   // working implementation (e.g. in-memory repo)
        MOCK,   // verifiable fake — records interactions
        SPY     // wraps real object; intercepts selected interactions
    }

    public record TestDoubleInfo(
            TestDoubleType type,
            String         description,
            String         mockitoCreation,
            String         bestUseCase) {}

    public static List<TestDoubleInfo> testDoubleGuide() {
        return List.of(
            new TestDoubleInfo(TestDoubleType.DUMMY,
                "A placeholder that satisfies type constraints but is never called",
                "No Mockito needed — pass null or new Object()",
                "Method signature satisfiers, parameter lists"),
            new TestDoubleInfo(TestDoubleType.STUB,
                "Returns fixed data; no interaction verification",
                "Mockito.when(stub.method()).thenReturn(value)",
                "Isolating code from slow or external dependencies"),
            new TestDoubleInfo(TestDoubleType.FAKE,
                "A lightweight working implementation for testing",
                "Implement interface manually (e.g. HashMap-backed repo)",
                "Repository tests, in-memory event buses"),
            new TestDoubleInfo(TestDoubleType.MOCK,
                "Verifies interactions — did method get called with expected args?",
                "Mockito.mock(SomeClass.class) or @Mock annotation",
                "Verifying service collaborator calls"),
            new TestDoubleInfo(TestDoubleType.SPY,
                "Wraps real object; only stubbed methods are overridden",
                "Mockito.spy(new RealObject()) or @Spy annotation",
                "Testing legacy code; partially overriding behaviour")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Argument captor guide
    // ─────────────────────────────────────────────────────────────────────────

    public record CaptorExample(
            String scenario,
            String captorDeclaration,
            String verifyLine,
            String assertionLine) {}

    public static List<CaptorExample> argumentCaptorExamples() {
        return List.of(
            new CaptorExample(
                "Capture a single value",
                "ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);",
                "verify(repo).save(captor.capture());",
                "assertEquals(\"PENDING\", captor.getValue().status());"),
            new CaptorExample(
                "Capture multiple calls",
                "ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);",
                "verify(emailService, times(3)).send(captor.capture());",
                "assertEquals(List.of(\"a@b.com\",\"c@d.com\",\"e@f.com\"), captor.getAllValues());"),
            new CaptorExample(
                "Capture and assert complex object",
                "ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);",
                "verify(bus).publish(captor.capture());",
                "assertThat(captor.getValue().type()).isEqualTo(\"ORDER_CREATED\");")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mockito patterns
    // ─────────────────────────────────────────────────────────────────────────

    public record MockitoPattern(String name, String codeSnippet, String purpose) {}

    public static List<MockitoPattern> mockitoPatterns() {
        return List.of(
            new MockitoPattern("when/thenReturn",
                "when(service.find(1L)).thenReturn(Optional.of(order));",
                "Stub a return value"),
            new MockitoPattern("when/thenThrow",
                "when(repo.save(any())).thenThrow(new DataIntegrityViolationException(\"dup\"));",
                "Stub an exception"),
            new MockitoPattern("doReturn (for spies)",
                "doReturn(42).when(spy).heavyComputation();",
                "Stub a spy method without executing the real method"),
            new MockitoPattern("verify",
                "verify(emailSvc, times(1)).send(eq(\"user@example.com\"), anyString());",
                "Assert a mock method was called"),
            new MockitoPattern("InOrder",
                "InOrder order = inOrder(step1, step2); order.verify(step1).execute(); order.verify(step2).execute();",
                "Assert methods were called in a specific sequence"),
            new MockitoPattern("ArgumentMatcher",
                "verify(repo).save(argThat(o -> o.total() > 0));",
                "Custom assertion on captured argument"),
            new MockitoPattern("thenAnswer",
                "when(idGen.next()).thenAnswer(inv -> UUID.randomUUID().toString());",
                "Dynamic return value each call")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mock vs Spy comparison
    // ─────────────────────────────────────────────────────────────────────────

    public record MockVsSpyInfo(String aspect, String mock, String spy) {}

    public static List<MockVsSpyInfo> mockVsSpy() {
        return List.of(
            new MockVsSpyInfo("Creation",
                "Mockito.mock(Clazz.class) — no real instance needed",
                "Mockito.spy(new Clazz()) — wraps a real instance"),
            new MockVsSpyInfo("Default behaviour",
                "Returns null/0/false for all unstubbed methods",
                "Calls real implementation for unstubbed methods"),
            new MockVsSpyInfo("Risk",
                "Low — nothing real executes",
                "Higher — real code runs; side effects possible"),
            new MockVsSpyInfo("Use when",
                "You want full isolation from the collaborator",
                "You want to override one method but keep the rest real")
        );
    }
}
