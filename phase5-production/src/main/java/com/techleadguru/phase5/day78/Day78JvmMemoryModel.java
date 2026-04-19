package com.techleadguru.phase5.day78;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAY 78 — JVM Memory Model: Trigger OutOfMemoryError per Region
 *
 * JVM MEMORY REGIONS (Java 11+):
 *
 *   Heap — split into Young Gen (Eden + Survivor) and Old Gen
 *     Minor GC: collects Young Gen
 *     Major/Full GC: collects Old/entire heap
 *
 *   Metaspace (off-heap) — class metadata, method bytecodes
 *     Was PermGen in Java 7. No fixed limit by default → can grow until native memory exhausted.
 *     Set -XX:MaxMetaspaceSize to cap it.
 *
 *   Thread Stack — each thread has its own fixed-size stack (default 256k–1M)
 *     StackOverflowError if recursion too deep.
 *
 *   Direct/Off-heap — ByteBuffer.allocateDirect(), NIO, Netty, Kafka client
 *     Lives outside GC → must be explicitly released or finalized.
 *
 * OutOfMemoryError VARIANTS:
 *   "Java heap space"             — heap exhausted (most common)
 *   "GC overhead limit exceeded"  — GC spending > 98% time with < 2% freed
 *   "Metaspace"                   — too many classes loaded (e.g., Groovy scripts)
 *   "Direct buffer memory"        — allocateDirect() used too much off-heap
 *   "unable to create new native thread" — too many threads, OS limit hit
 *
 * JVM FLAGS:
 *   -Xmx512m               — max heap
 *   -XX:MaxMetaspaceSize=128m  — cap metaspace
 *   -XX:+HeapDumpOnOutOfMemoryError  — auto heap dump on OOM
 *   -XX:HeapDumpPath=/tmp/app.hprof  — where to write it
 */
@Slf4j
public class Day78JvmMemoryModel {

    // =========================================================================
    // Memory usage reporter — uses JMX to read live region sizes
    // =========================================================================

    public static class MemoryReporter {

        public static MemorySnapshot snapshot() {
            MemoryMXBean mem        = ManagementFactory.getMemoryMXBean();
            MemoryUsage heap        = mem.getHeapMemoryUsage();
            MemoryUsage nonHeap     = mem.getNonHeapMemoryUsage();
            List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();

            long heapUsed    = heap.getUsed();
            long heapMax     = heap.getMax();
            long metaUsed    = 0L;
            long metaMax     = 0L;

            for (MemoryPoolMXBean pool : pools) {
                String name = pool.getName().toLowerCase();
                if (name.contains("metaspace")) {
                    metaUsed = pool.getUsage().getUsed();
                    metaMax  = pool.getUsage().getMax(); // -1 if no limit set
                }
            }

            return new MemorySnapshot(heapUsed, heapMax, nonHeap.getUsed(), metaUsed, metaMax);
        }

        public record MemorySnapshot(long heapUsed, long heapMax, long nonHeapUsed,
                                     long metaUsed, long metaMax) {
            public long heapUsedMb()    { return heapUsed    / 1024 / 1024; }
            public long heapMaxMb()     { return heapMax     / 1024 / 1024; }
            public long nonHeapUsedMb() { return nonHeapUsed / 1024 / 1024; }
            public long metaUsedKb()    { return metaUsed    / 1024; }

            @Override
            public String toString() {
                return String.format(
                    "Heap: %d/%d MB | NonHeap(NativeArea+JIT): %d MB | Metaspace: %d KB",
                    heapUsedMb(), heapMaxMb(), nonHeapUsedMb(), metaUsedKb()
                );
            }
        }
    }

    // =========================================================================
    // Heap leak simulator — allocates chunks but never releases them
    // =========================================================================

    public static class HeapLeakSimulator {

        private final List<byte[]> leakedChunks = new ArrayList<>();

        /**
         * Allocates {@code chunkSizeMb} MB and holds a reference to it.
         * Calling this repeatedly will exhaust the heap.
         */
        public long allocateChunkMb(int chunkSizeMb) {
            byte[] chunk = new byte[chunkSizeMb * 1024 * 1024];
            leakedChunks.add(chunk);
            log.info("[Day78] Allocated {}MB chunk. Total held: {} MB",
                    chunkSizeMb, leakedChunks.size() * chunkSizeMb);
            return leakedChunks.size() * (long) chunkSizeMb;
        }

        public int getChunkCount()          { return leakedChunks.size(); }
        public void clear()                  { leakedChunks.clear(); }
    }

    // =========================================================================
    // Stack overflow demo
    // =========================================================================

    public static class StackDemo {

        /** Causes StackOverflowError if maxDepth is very large. */
        public static int recurse(int depth, int maxDepth) {
            if (depth >= maxDepth) return depth;
            return recurse(depth + 1, maxDepth);
        }
    }

    // =========================================================================
    // REST controller to trigger demos safely
    // =========================================================================

    @RestController
    @RequestMapping("/api/day78/memory")
    @Slf4j
    public static class MemoryController {

        private final HeapLeakSimulator simulator = new HeapLeakSimulator();

        @GetMapping("/snapshot")
        public MemoryReporter.MemorySnapshot snapshot() {
            MemoryReporter.MemorySnapshot snap = MemoryReporter.snapshot();
            log.info("[Day78] Memory snapshot: {}", snap);
            return snap;
        }

        @PostMapping("/allocate/{mb}")
        public String allocate(@PathVariable int mb) {
            if (mb > 100) return "Refused: max 100 MB per call to protect test JVM";
            long totalMb = simulator.allocateChunkMb(mb);
            return "Allocated " + mb + "MB. Total held: " + totalMb + "MB";
        }

        @DeleteMapping("/clear")
        public String clear() {
            simulator.clear();
            System.gc();
            return "Cleared all leaked chunks";
        }

        @GetMapping("/recurse/{depth}")
        public String recurse(@PathVariable int depth) {
            if (depth > 10_000) return "Refused: max depth 10000";
            try {
                int reached = StackDemo.recurse(0, depth);
                return "Reached depth " + reached + " safely";
            } catch (StackOverflowError e) {
                return "StackOverflowError at depth ~ " + depth;
            }
        }
    }
}
