package com.techleadguru.phase8.day162;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class Day162SagaPatternTest {

    @Test
    void sagaRecordsStepsAndRollsBackInReverseOrder() {
        var saga = new Day162SagaPattern.SagaInstance("saga-1");
        saga.recordStep("reserveInventory",  "releaseInventory");
        saga.recordStep("debitPayment",      "refundPayment");
        saga.recordStep("createShipment",    "cancelShipment");

        assertEquals(3, saga.stepCount());

        List<String> comp = saga.rollback();

        assertTrue(saga.isRolledBack());
        assertEquals(List.of("cancelShipment", "refundPayment", "releaseInventory"), comp);
    }

    @Test
    void orchestratorRunsAllStepsOnSuccess() {
        AtomicInteger counter = new AtomicInteger();

        var orchestrator = new Day162SagaPattern.SagaOrchestrator()
                .addStep(new Day162SagaPattern.SagaStepExecutor() {
                    public boolean execute(Day162SagaPattern.SagaInstance s) {
                        counter.incrementAndGet(); return true;
                    }
                    public String stepName() { return "step1"; }
                    public String compensationDescription() { return "undo1"; }
                })
                .addStep(new Day162SagaPattern.SagaStepExecutor() {
                    public boolean execute(Day162SagaPattern.SagaInstance s) {
                        counter.incrementAndGet(); return true;
                    }
                    public String stepName() { return "step2"; }
                    public String compensationDescription() { return "undo2"; }
                });

        var result = orchestrator.execute("saga-ok");

        assertFalse(result.isRolledBack());
        assertEquals(2, result.stepCount());
        assertEquals(2, counter.get());
    }

    @Test
    void orchestratorRollsBackWhenStepFails() {
        var orchestrator = new Day162SagaPattern.SagaOrchestrator()
                .addStep(new Day162SagaPattern.SagaStepExecutor() {
                    public boolean execute(Day162SagaPattern.SagaInstance s) { return true; }
                    public String stepName() { return "step1"; }
                    public String compensationDescription() { return "undo1"; }
                })
                .addStep(new Day162SagaPattern.SagaStepExecutor() {
                    public boolean execute(Day162SagaPattern.SagaInstance s) { return false; }
                    public String stepName() { return "step2"; }
                    public String compensationDescription() { return "undo2"; }
                });

        var result = orchestrator.execute("saga-fail");

        assertTrue(result.isRolledBack());
        assertEquals(1, result.stepCount()); // only step1 completed before failure
    }

    @Test
    void eventBusRoutesEventsToSubscribers() {
        var bus = new Day162SagaPattern.EventBus();
        AtomicInteger counter = new AtomicInteger();

        bus.subscribe("ORDER_CREATED", e -> counter.incrementAndGet());
        bus.subscribe("ORDER_CREATED", e -> counter.incrementAndGet());
        bus.subscribe("PAYMENT_DONE",  e -> counter.addAndGet(100));

        bus.publish(new Day162SagaPattern.DomainEvent("ORDER_CREATED", "saga-1", "{}"));

        assertEquals(2, counter.get());
        assertEquals(1, bus.published().size());
    }

    @Test
    void comparisonListIsNonEmpty() {
        var list = Day162SagaPattern.comparison();
        assertFalse(list.isEmpty());
        list.forEach(c -> {
            assertNotNull(c.aspect());
            assertNotNull(c.choreography());
            assertNotNull(c.orchestration());
        });
    }
}
