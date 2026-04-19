package com.techleadguru.phase7.day154;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Day154KafkaStreamsTest {

    @Test
    void testTumblingWindowAggregatesEvents() {
        // Window = 60_000 ms
        Day154KafkaStreams.TumblingWindowSimulator sim =
                new Day154KafkaStreams.TumblingWindowSimulator(60_000L);
        // All events in the same 60s window (0-59999ms)
        sim.addEvent("word", 1_000L);
        sim.addEvent("word", 30_000L);
        sim.addEvent("word", 59_000L);

        List<Day154KafkaStreams.WindowedCount> counts = sim.getWindowCounts();
        assertEquals(1, counts.size());
        assertEquals("word", counts.get(0).key());
        assertEquals(3L, counts.get(0).count());
    }

    @Test
    void testTumblingWindowSeparatesEvents() {
        Day154KafkaStreams.TumblingWindowSimulator sim =
                new Day154KafkaStreams.TumblingWindowSimulator(60_000L);

        sim.addEvent("click", 1_000L);     // window 0
        sim.addEvent("click", 70_000L);    // window 60_000

        List<Day154KafkaStreams.WindowedCount> counts = sim.getWindowCounts();
        assertEquals(2, counts.size());
        counts.forEach(c -> assertEquals(1L, c.count()));
    }

    @Test
    void testMultipleKeys() {
        Day154KafkaStreams.TumblingWindowSimulator sim =
                new Day154KafkaStreams.TumblingWindowSimulator(60_000L);
        sim.addEvent("foo", 0L);
        sim.addEvent("bar", 0L);
        sim.addEvent("foo", 1_000L);

        List<Day154KafkaStreams.WindowedCount> counts = sim.getWindowCounts();
        Day154KafkaStreams.WindowedCount fooCount = counts.stream()
                .filter(c -> "foo".equals(c.key())).findFirst().orElseThrow();
        assertEquals(2L, fooCount.count());
    }

    @Test
    void testWindowStartAndEnd() {
        Day154KafkaStreams.TumblingWindowSimulator sim =
                new Day154KafkaStreams.TumblingWindowSimulator(60_000L);
        sim.addEvent("k", 0L);

        Day154KafkaStreams.WindowedCount wc = sim.getWindowCounts().get(0);
        assertEquals(0L, wc.windowStart().toEpochMilli());
        assertEquals(60_000L, wc.windowEnd().toEpochMilli());
    }

    @Test
    void testWindowTypes() {
        List<Day154KafkaStreams.WindowType> types = Day154KafkaStreams.windowTypes();
        assertEquals(4, types.size());
        assertTrue(types.stream().anyMatch(t -> t.name().equals("Tumbling")));
    }

    @Test
    void testStreamsVsConsumer() {
        List<Day154KafkaStreams.StreamsComparison> comparisons =
                Day154KafkaStreams.streamsVsConsumer();
        assertEquals(5, comparisons.size());
    }
}
