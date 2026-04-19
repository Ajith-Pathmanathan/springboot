package com.techleadguru.phase7.day154;

import java.time.Instant;
import java.util.*;

/**
 * Day 154 — Kafka Streams with windowed operations
 *
 * Kafka Streams: stream processing library built on top of the Kafka client.
 * Processes records directly from Kafka topics using a topology of operations.
 *
 * Window types:
 *   - Tumbling: fixed size, non-overlapping
 *   - Hopping (sliding): fixed size, overlapping (advances by hop)
 *   - Session: variable-size, grouped by inactivity gap
 */
public class Day154KafkaStreams {

    // ─────────────────────────────────────────────────────────────────────────
    // Windowed count result
    // ─────────────────────────────────────────────────────────────────────────

    public record WindowedCount(
            Instant windowStart,
            Instant windowEnd,
            String  key,
            long    count) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Topology descriptor (documentation only — no real DSL dependency)
    // ─────────────────────────────────────────────────────────────────────────

    public record StreamsTopology(
            String sourceTopic,
            String sinkTopic,
            String description,
            String pseudoCode) {}

    public static StreamsTopology wordCountTopology() {
        return new StreamsTopology(
            "text-input",
            "word-counts",
            "Count word occurrences in a tumbling 60-second window",
            """
            builder.stream("text-input")
                .flatMapValues(v -> Arrays.asList(v.split("\\s+")))
                .groupBy((k, word) -> word)
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(60)))
                .count(Materialized.as("word-count-store"))
                .toStream()
                .to("word-counts", Produced.with(windowedSerde, longSerde));
            """
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tumbling window simulator (pure Java)
    // ─────────────────────────────────────────────────────────────────────────

    public static class TumblingWindowSimulator {

        private final long                           windowSizeMs;
        private final Map<String, Map<Long, Long>>   counts = new LinkedHashMap<>();
        // counts[key][windowStart] = count

        public TumblingWindowSimulator(long windowSizeMs) {
            this.windowSizeMs = windowSizeMs;
        }

        /** Add an event for the given key at the given epoch-ms timestamp. */
        public void addEvent(String key, long timestampMs) {
            long windowStart = (timestampMs / windowSizeMs) * windowSizeMs;
            counts.computeIfAbsent(key, k -> new TreeMap<>())
                  .merge(windowStart, 1L, Long::sum);
        }

        /** Returns all window counts across all keys sorted by window start. */
        public List<WindowedCount> getWindowCounts() {
            List<WindowedCount> result = new ArrayList<>();
            for (Map.Entry<String, Map<Long, Long>> keyEntry : counts.entrySet()) {
                for (Map.Entry<Long, Long> windowEntry : keyEntry.getValue().entrySet()) {
                    long start = windowEntry.getKey();
                    result.add(new WindowedCount(
                            Instant.ofEpochMilli(start),
                            Instant.ofEpochMilli(start + windowSizeMs),
                            keyEntry.getKey(),
                            windowEntry.getValue()));
                }
            }
            result.sort(Comparator.comparing(WindowedCount::windowStart));
            return result;
        }

        public void clear() { counts.clear(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Window types
    // ─────────────────────────────────────────────────────────────────────────

    public record WindowType(String name, String description, String kafkaStreamsClass) {}

    public static List<WindowType> windowTypes() {
        return List.of(
            new WindowType("Tumbling",
                "Fixed-size, non-overlapping windows. Each event in exactly one window.",
                "TimeWindows.ofSizeWithNoGrace(Duration)"),
            new WindowType("Hopping",
                "Fixed-size, overlapping windows. Advance by 'hop' interval. Event may appear in multiple windows.",
                "TimeWindows.of(size).advanceBy(hop)"),
            new WindowType("Sliding",
                "Window defined by a time difference between events; used with KStream.join.",
                "SlidingWindows.ofTimeDifferenceWithNoGrace(timeDifference)"),
            new WindowType("Session",
                "Variable-size; grouped by inactivity gap. Merges windows when new event is within gap.",
                "SessionWindows.ofInactivityGapWithNoGrace(Duration)")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Kafka Streams vs plain consumer comparison
    // ─────────────────────────────────────────────────────────────────────────

    public record StreamsComparison(String aspect, String kafkaStreams, String kafkaConsumer) {}

    public static List<StreamsComparison> streamsVsConsumer() {
        return List.of(
            new StreamsComparison("Windowing",
                "Built-in: tumbling, hopping, session",
                "Must implement manually with state"),
            new StreamsComparison("State stores",
                "Managed in-process RocksDB; backed by changelog topic",
                "Manual (external DB / in-memory)"),
            new StreamsComparison("Join operations",
                "Stream-stream, stream-table, table-table joins",
                "Manual lookup to external store"),
            new StreamsComparison("Fault tolerance",
                "Automatic via changelog topics and task reassignment",
                "Manual offset management and restart logic"),
            new StreamsComparison("Deployment",
                "Embedded library (no separate cluster); runs inside your app",
                "Same — embedded consumer")
        );
    }
}
