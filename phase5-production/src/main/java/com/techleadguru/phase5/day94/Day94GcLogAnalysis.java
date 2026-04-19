package com.techleadguru.phase5.day94;

import java.time.Instant;
import java.util.*;
import java.util.regex.*;

/**
 * DAY 94 — GC Log Analysis
 *
 * ENABLE GC LOGGING (JVM args):
 *   -Xlog:gc*:file=/var/log/app/gc.log:time,uptime,level,tags:filecount=5,filesize=20m
 *
 * SAMPLE G1GC LOG LINES:
 *   [2024-01-15T10:23:45.123+0000][1.234s][info][gc] GC(42) Pause Young (Normal) (G1 Evacuation Pause) 512M->480M(8192M) 12.345ms
 *   [2024-01-15T10:23:55.456+0000][11.567s][info][gc] GC(43) Pause Young (Concurrent Start) 480M->460M(8192M) 8.123ms
 *   [2024-01-15T10:24:05.789+0000][21.900s][warn][gc] GC(44) Pause Full (G1 Evacuation Failure) 7800M->3200M(8192M) 15234.567ms
 *
 * ANALYSIS WORKFLOW:
 *   1. Parse log → entries with (timestamp, type, beforeMb, afterMb, pauseMs)
 *   2. Find high-pause events (> threshold)
 *   3. Compute GC overhead % (sum(pauseMs) / windowMs * 100)
 *   4. Detect Full GC (pause > 1000ms or type contains "Full")
 *   5. Plot pause distribution (p50, p95, p99)
 *
 * KEY SIGNALS:
 *   GC overhead > 5%  → heap too small, or severe allocation rate
 *   Full GC present   → Old Gen filling up → application pause
 *   Heap after GC growing over time → memory leak
 *   Many humongous allocations → large arrays allocated frequently (tune G1HeapRegionSize)
 *
 * TOOLS:
 *   GCViewer (open source)
 *   GCEasy (web-based, freemium)
 *   IntelliJ  profiler → GC tab
 */
public class Day94GcLogAnalysis {

    // =========================================================================
    // Log entry model
    // =========================================================================

    public record GcLogEntry(Instant timestamp, double uptimeSec, String type,
                             int heapBeforeMb, int heapAfterMb, int heapTotalMb,
                             double pauseMs) {

        public boolean isFull() {
            return pauseMs > 1000.0 || type.contains("Full");
        }

        public int reclaimedMb() {
            return heapBeforeMb - heapAfterMb;
        }
    }

    // =========================================================================
    // Parser for unified JVM logging format (-Xlog:gc*)
    // =========================================================================

    public static class GcLogParser {

        // Pattern: [timestamp][uptime][level][gc] GC(N) Pause <type>... beforeM->afterM(totalM) pauseMs
        private static final Pattern PATTERN = Pattern.compile(
                "\\[(\\d{4}-\\d{2}-\\d{2}T[^\\]]+)\\]" +   // timestamp
                "\\[(\\d+\\.\\d+)s\\]" +                     // uptime
                ".*?GC\\(\\d+\\) Pause (\\S+(?:\\s\\S+)*) " + // pause type (greedy until heap sizes)
                "(\\d+)M->(\\d+)M\\((\\d+)M\\) " +          // heapBefore->heapAfter(total)
                "([\\d.]+)ms"                                 // pause duration
        );

        /**
         * Parse a single GC log line. Returns empty Optional for non-GC lines.
         */
        public static Optional<GcLogEntry> parseEntry(String line) {
            Matcher m = PATTERN.matcher(line);
            if (!m.find()) return Optional.empty();
            try {
                Instant ts = Instant.parse(m.group(1));
                double uptime   = Double.parseDouble(m.group(2));
                String type     = m.group(3).trim();
                int before      = Integer.parseInt(m.group(4));
                int after       = Integer.parseInt(m.group(5));
                int total       = Integer.parseInt(m.group(6));
                double pause    = Double.parseDouble(m.group(7));
                return Optional.of(new GcLogEntry(ts, uptime, type, before, after, total, pause));
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        public static List<GcLogEntry> parseAll(List<String> lines) {
            List<GcLogEntry> result = new ArrayList<>();
            for (String line : lines) {
                parseEntry(line).ifPresent(result::add);
            }
            return result;
        }
    }

    // =========================================================================
    // Analysis functions
    // =========================================================================

    public static class GcAnalyzer {

        /**
         * Find GC events where pause exceeded the threshold.
         */
        public static List<GcLogEntry> findHighPausePeriods(List<GcLogEntry> entries,
                                                             double thresholdMs) {
            return entries.stream()
                    .filter(e -> e.pauseMs() > thresholdMs)
                    .toList();
        }

        /**
         * Average pause across all GC events.
         */
        public static double computeAveragePause(List<GcLogEntry> entries) {
            return entries.stream()
                    .mapToDouble(GcLogEntry::pauseMs)
                    .average()
                    .orElse(0.0);
        }

        /**
         * GC overhead = totalPauseMs / totalWindowMs * 100
         * windowMs is the elapsed time of the log window.
         */
        public static double computeGcOverheadPercent(List<GcLogEntry> entries, double windowMs) {
            if (windowMs <= 0) return 0.0;
            double totalPause = entries.stream().mapToDouble(GcLogEntry::pauseMs).sum();
            return totalPause / windowMs * 100.0;
        }

        /**
         * Percentile pause (0.95 → 95th percentile).
         */
        public static double pausePercentile(List<GcLogEntry> entries, double percentile) {
            if (entries.isEmpty()) return 0.0;
            List<Double> pauses = entries.stream()
                    .map(GcLogEntry::pauseMs)
                    .sorted()
                    .toList();
            int idx = (int) Math.ceil(percentile * pauses.size()) - 1;
            return pauses.get(Math.max(0, Math.min(idx, pauses.size() - 1)));
        }

        /**
         * Detect whether heap-after-GC is trending upward (memory leak signal).
         */
        public static boolean isHeapTrendingUp(List<GcLogEntry> entries) {
            if (entries.size() < 4) return false;
            // Compare average of first quarter vs last quarter
            int quarter = entries.size() / 4;
            double earlyAvg = entries.subList(0, quarter).stream()
                    .mapToInt(GcLogEntry::heapAfterMb).average().orElse(0);
            double lateAvg = entries.subList(entries.size() - quarter, entries.size()).stream()
                    .mapToInt(GcLogEntry::heapAfterMb).average().orElse(0);
            return lateAvg > earlyAvg * 1.15; // 15% growth threshold
        }

        public record GcSummary(int totalEvents, int fullGcEvents, double avgPauseMs,
                                double p95PauseMs, double p99PauseMs, double gcOverheadPct,
                                boolean heapTrendingUp) {}

        public static GcSummary summarize(List<GcLogEntry> entries) {
            if (entries.isEmpty()) {
                return new GcSummary(0, 0, 0, 0, 0, 0, false);
            }
            double windowMs = (entries.getLast().uptimeSec() - entries.getFirst().uptimeSec()) * 1000.0;
            return new GcSummary(
                    entries.size(),
                    (int) entries.stream().filter(GcLogEntry::isFull).count(),
                    computeAveragePause(entries),
                    pausePercentile(entries, 0.95),
                    pausePercentile(entries, 0.99),
                    computeGcOverheadPercent(entries, windowMs),
                    isHeapTrendingUp(entries)
            );
        }
    }
}
