package com.techleadguru.phase8.day162;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Day 162 — Saga Pattern: Choreography vs Orchestration
 *
 * A Saga is a sequence of local transactions; each step publishes an event.
 * On failure, compensating transactions undo previous steps.
 *
 * Choreography: each service reacts to events and emits new events.
 *   + Simple, decoupled
 *   - Hard to trace, cyclic dependencies possible
 *
 * Orchestration: a central coordinator (Saga Orchestrator) tells services
 * what to do and handles rollback.
 *   + Clear flow, easy to monitor
 *   - Central coordinator can become a bottleneck
 */
public class Day162SagaPattern {

    // ─────────────────────────────────────────────────────────────────────────
    // Saga step result
    // ─────────────────────────────────────────────────────────────────────────

    public enum StepStatus { PENDING, COMPLETED, COMPENSATED, FAILED }

    public record SagaStep(
            String     name,
            StepStatus status,
            String     compensationAction) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Saga instance
    // ─────────────────────────────────────────────────────────────────────────

    public static class SagaInstance {

        private final String sagaId;
        private final List<SagaStep> completedSteps = new ArrayList<>();
        private volatile boolean rolledBack = false;

        public SagaInstance(String sagaId) {
            this.sagaId = sagaId;
        }

        public void recordStep(String stepName, String compensationAction) {
            completedSteps.add(new SagaStep(stepName, StepStatus.COMPLETED, compensationAction));
        }

        /** Simulates compensating all completed steps in reverse order. */
        public List<String> rollback() {
            rolledBack = true;
            List<String> compensations = new ArrayList<>();
            List<SagaStep> reversed = new ArrayList<>(completedSteps);
            Collections.reverse(reversed);
            reversed.forEach(step -> compensations.add(step.compensationAction()));
            return compensations;
        }

        public String sagaId()                 { return sagaId; }
        public boolean isRolledBack()          { return rolledBack; }
        public List<SagaStep> completedSteps() { return Collections.unmodifiableList(completedSteps); }
        public int stepCount()                 { return completedSteps.size(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Orchestration-style saga executor
    // ─────────────────────────────────────────────────────────────────────────

    public interface SagaStepExecutor {
        /** Returns true if the step succeeded, false to trigger rollback. */
        boolean execute(SagaInstance saga);
        String stepName();
        String compensationDescription();
    }

    public static class SagaOrchestrator {

        private final List<SagaStepExecutor> steps = new ArrayList<>();

        public SagaOrchestrator addStep(SagaStepExecutor step) {
            steps.add(step);
            return this;
        }

        /** Run the saga. Returns the saga instance (check isRolledBack()). */
        public SagaInstance execute(String sagaId) {
            SagaInstance saga = new SagaInstance(sagaId);
            for (SagaStepExecutor step : steps) {
                boolean success = step.execute(saga);
                if (success) {
                    saga.recordStep(step.stepName(), step.compensationDescription());
                } else {
                    saga.rollback();
                    return saga;
                }
            }
            return saga;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Event bus (choreography)
    // ─────────────────────────────────────────────────────────────────────────

    public record DomainEvent(String eventType, String sagaId, String payload) {}

    public static class EventBus {

        private final Map<String, List<java.util.function.Consumer<DomainEvent>>> handlers =
                new LinkedHashMap<>();
        private final List<DomainEvent> published = new CopyOnWriteArrayList<>();

        public void subscribe(String eventType,
                              java.util.function.Consumer<DomainEvent> handler) {
            handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
        }

        public void publish(DomainEvent event) {
            published.add(event);
            handlers.getOrDefault(event.eventType(), List.of())
                    .forEach(h -> h.accept(event));
        }

        public List<DomainEvent> published() { return Collections.unmodifiableList(published); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Comparison guide
    // ─────────────────────────────────────────────────────────────────────────

    public record SagaComparison(
            String aspect,
            String choreography,
            String orchestration) {}

    public static List<SagaComparison> comparison() {
        return List.of(
            new SagaComparison("Coupling",
                "Services coupled via events only — highly decoupled",
                "Services only know the orchestrator — less event coupling"),
            new SagaComparison("Visibility",
                "Hard to see overall saga state; need tracing across events",
                "Orchestrator holds state; easy to monitor and debug"),
            new SagaComparison("Rollback",
                "Each service emits compensation event; others react",
                "Orchestrator sends explicit compensation commands"),
            new SagaComparison("Complexity",
                "Simple per-service logic; complex cross-service flow",
                "Central coordinator is complex but flow is explicit"),
            new SagaComparison("Test strategy",
                "Test each service's event reaction in isolation",
                "Test orchestrator with mock service stubs")
        );
    }
}
