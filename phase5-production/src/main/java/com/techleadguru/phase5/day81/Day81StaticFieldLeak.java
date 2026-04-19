package com.techleadguru.phase5.day81;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * DAY 81 — Static Field Memory Leak
 *
 * PATTERN: A static field holds references to objects that should be
 * garbage-collected, but the static reference prevents GC.
 *
 * COMMON CAUSES:
 *   1. Cache: static Map<K,V> — entries added, never removed
 *   2. Logger / config: static list of registered listeners
 *   3. Singleton with mutable state: static counter + history list
 *   4. Inner class reference to outer (anonymous Runnable capturing `this`)
 *
 * HOW TO DETECT:
 *   - jmap histogram: high instance count for your model class (e.g., "Report")
 *   - MAT dominator tree: static HashMap at top, retaining 80% of heap
 *   - Heap grows monotonically between GCs, never returns to baseline
 *
 * HOW TO FIX:
 *   - Bounded cache: Caffeine with maximumSize + expireAfterWrite (Day 84)
 *   - WeakHashMap: entries auto-removed when key has no strong references
 *   - Explicit eviction: remove(id) after use
 *   - Move to instance field (bean scope): if it only needs to live per-request
 */
@Slf4j
public class Day81StaticFieldLeak {

    // =========================================================================
    // BROKEN: Unbounded static cache
    // =========================================================================

    @Slf4j
    @Component
    public static class ReportService {

        // BUG: this map is never cleared → grows with every report generation
        private static final Map<String, Report> inMemoryReportCache = new HashMap<>();

        public Report generateAndCache(String reportId) {
            Report r = new Report(reportId, new byte[10 * 1024]); // 10KB per report
            inMemoryReportCache.put(reportId, r);
            log.debug("[Day81] Cached report {}. Total cached: {}", reportId, inMemoryReportCache.size());
            return r;
        }

        public int getCacheSize() { return inMemoryReportCache.size(); }
        public static void clearCache() { inMemoryReportCache.clear(); }

        public record Report(String id, byte[] data) {}
    }

    // =========================================================================
    // BROKEN: Static listener list — classic framework / event bus leak
    // =========================================================================

    @Slf4j
    public static class EventBus {

        // BUG: listeners are registered but never de-registered
        // When a Controller is garbage-collected, the EventBus still holds a reference
        private static final List<EventListener> listeners = new CopyOnWriteArrayList<>();

        public static void register(EventListener l) {
            listeners.add(l);
            log.debug("[Day81] Registered listener. Total: {}", listeners.size());
        }

        /** BROKEN: no way to remove from outside → leak */
        public static void publish(String event) {
            listeners.forEach(l -> l.onEvent(event));
        }

        public static int listenerCount() { return listeners.size(); }
        public static void clearAll()     { listeners.clear(); } // test cleanup only

        public interface EventListener {
            void onEvent(String event);
        }
    }

    // =========================================================================
    // FIXED: WeakReference-based listener list
    // =========================================================================

    @Slf4j
    public static class SafeEventBus {

        // WeakReference: if listener has no other strong reference → auto-removed by GC
        private static final List<java.lang.ref.WeakReference<EventBus.EventListener>>
                weakListeners = new CopyOnWriteArrayList<>();

        public static void register(EventBus.EventListener l) {
            weakListeners.add(new java.lang.ref.WeakReference<>(l));
        }

        public static void publish(String event) {
            // Iterate and remove stale (GC'd) references at the same time
            weakListeners.removeIf(ref -> {
                EventBus.EventListener l = ref.get();
                if (l == null) return true; // GC'd — remove from list
                l.onEvent(event);
                return false;
            });
        }

        public static int listenerCount() {
            return (int) weakListeners.stream().filter(r -> r.get() != null).count();
        }

        public static void clearAll() { weakListeners.clear(); }
    }

    // =========================================================================
    // Inner class capturing outer object reference — subtle leak
    // =========================================================================

    @Slf4j
    public static class InnerClassLeakDemo {

        private final String name;
        private final byte[] largeData;

        public InnerClassLeakDemo(String name) {
            this.name = name;
            this.largeData = new byte[1024 * 1024]; // 1MB
        }

        /**
         * BROKEN: anonymous Runnable is a non-static inner class.
         * It implicitly holds a reference to `this` (the outer InnerClassLeakDemo).
         * If the Runnable is stored somewhere (e.g., EventBus), the outer object leaks.
         */
        public Runnable createLeakyTask() {
            return () -> log.debug("[Day81] Running task for: {}", name); // captures `this`
        }

        /**
         * FIXED: static inner class / lambda that doesn't capture outer state.
         * Only captures what it needs (just the name String, not `this`).
         */
        public Runnable createSafeTask() {
            String capturedName = this.name; // copy primitive/immutable
            return () -> log.debug("[Day81] Running SAFE task for: {}", capturedName);
        }

        public int getDataSize() { return largeData.length; }
    }
}
