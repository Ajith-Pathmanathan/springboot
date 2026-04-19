package com.techleadguru.phase5.day95;

import java.lang.management.*;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * DAY 95 — Off-Heap Memory: DirectByteBuffer and Native Memory Tracking
 *
 * THE PROBLEM WITH HEAP-ONLY THINKING:
 *   Java processes can consume 2x-3x their -Xmx value in total RSS.
 *   Why? Memory outside the heap:
 *   ┌─────────────────────────────────────────────────────────┐
 *   │  Heap (Xmx)                                             │
 *   │  Metaspace — class metadata (grows with many classes)   │
 *   │  Code Cache — JIT compiled code                         │
 *   │  Thread Stacks — ~256KB–1MB per thread                  │
 *   │  Direct Buffers — ByteBuffer.allocateDirect()           │
 *   │  Mapped Files — MappedByteBuffer                        │
 *   │  JNI memory — native libraries (e.g. netty, RocksDB)   │
 *   └─────────────────────────────────────────────────────────┘
 *
 * DIRECTBYTEBUFFER:
 *   ByteBuffer.allocateDirect(size) — allocates outside heap
 *   Used by: NIO channels, Netty (pooled allocator), Kafka, RocksDB
 *   Advantages: zero-copy to OS (no heap copy for network I/O)
 *   Risks: not GC'd by Young Gen; freed only when DirectByteBuffer referent is GC'd
 *          → can fill up before GC runs → OutOfMemoryError: Direct buffer memory
 *
 *   -XX:MaxDirectMemorySize=512m  — limit direct memory (default = Xmx)
 *
 * NATIVE MEMORY TRACKING (NMT):
 *   JVM flag: -XX:NativeMemoryTracking=summary  (or detail)
 *   Command:  jcmd <pid> VM.native_memory summary
 *   Outputs: Heap, Class, Thread, Code, GC, Internal, Symbol, Native, Arena...
 *
 * METASPACE OOM:
 *   Error: java.lang.OutOfMemoryError: Metaspace
 *   Cause: class loading leak (e.g. new classloader per request, groovy scripts)
 *   Fix: -XX:MaxMetaspaceSize=256m + investigate class loading with JFR
 *
 * NIO MEMORY LEAK PATTERN:
 *   for (;;) { ByteBuffer.allocateDirect(1 * 1024 * 1024); }  // no reference kept
 *   → GC may not run fast enough to free old buffers before new ones are needed
 *   Fix: explicitly call ((sun.misc.Cleaner)...cleaner()).clean() or use a buffer pool
 */
public class Day95OffHeap {

    // =========================================================================
    // DirectByteBuffer demo
    // =========================================================================

    public static class DirectBufferDemo {

        private ByteBuffer buffer;
        private final int size;

        public DirectBufferDemo(int bytes) {
            this.size = bytes;
        }

        /**
         * Allocate a direct ByteBuffer of the configured size.
         * Allocated outside the Java heap.
         */
        public void allocate() {
            this.buffer = ByteBuffer.allocateDirect(size);
        }

        /**
         * Write and read bytes to verify the buffer works.
         */
        public boolean readWrite() {
            if (buffer == null) throw new IllegalStateException("Call allocate() first");
            byte[] data = "Hello Direct Memory".getBytes();
            buffer.clear();
            buffer.put(data);
            buffer.flip();
            byte[] readBack = new byte[data.length];
            buffer.get(readBack);
            return Arrays.equals(data, readBack);
        }

        /**
         * Release direct memory immediately using the Cleaner.
         * Without this, memory is freed when buffer is GC'd — which may be too late.
         *
         * Note: This uses reflection against internal APIs (acceptable in JDK 11+
         * with --add-opens, but risky in production).
         * Better approach in real code: use a buffer pool (Netty PooledByteBufAllocator).
         */
        public void release() {
            if (buffer != null && buffer.isDirect()) {
                try {
                    Method cleanerMethod = buffer.getClass().getMethod("cleaner");
                    cleanerMethod.setAccessible(true);
                    Object cleaner = cleanerMethod.invoke(buffer);
                    if (cleaner != null) {
                        Method cleanMethod = cleaner.getClass().getMethod("clean");
                        cleanMethod.setAccessible(true);
                        cleanMethod.invoke(cleaner);
                    }
                } catch (Exception e) {
                    // Cleaner API not accessible — just null the reference and let GC handle it
                }
                buffer = null;
            }
        }

        public boolean isAllocated() { return buffer != null; }
        public int getSize()         { return size; }
    }

    // =========================================================================
    // Native memory stats via JMX
    // =========================================================================

    public static class NativeMemoryStats {

        /**
         * NMT requires -XX:NativeMemoryTracking=summary at startup.
         * This checks if the flag is active.
         */
        public static boolean trackingEnabled() {
            return ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                    .anyMatch(arg -> arg.contains("NativeMemoryTracking"));
        }

        /**
         * Returns current non-heap (off-heap JVM internal) memory committed in MB.
         * This includes metaspace, code cache, etc — but NOT direct buffers.
         */
        public static long nonHeapCommittedMb() {
            MemoryUsage usage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
            return usage.getCommitted() / (1024 * 1024);
        }

        /**
         * Metaspace usage via MemoryPoolMXBeans.
         */
        public static Optional<MemoryUsage> metaspaceUsage() {
            return ManagementFactory.getMemoryPoolMXBeans().stream()
                    .filter(p -> p.getName().contains("Metaspace"))
                    .map(MemoryPoolMXBean::getUsage)
                    .findFirst();
        }

        public record MemorySummary(long heapUsedMb, long heapMaxMb,
                                    long nonHeapCommittedMb, long metaspaceUsedMb,
                                    long directBufferEstimateMb) {}

        public static MemorySummary summarize() {
            MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            long heapUsed   = heap.getUsed() / (1024 * 1024);
            long heapMax    = heap.getMax() / (1024 * 1024);
            long nonHeap    = nonHeapCommittedMb();
            long metaspace  = metaspaceUsage()
                    .map(u -> u.getUsed() / (1024 * 1024))
                    .orElse(0L);
            // Direct buffers appear in sun.management.ManagementFactoryHelper — estimate via JMX
            long directEstimate = estimateDirectBufferMb();
            return new MemorySummary(heapUsed, heapMax, nonHeap, metaspace, directEstimate);
        }

        private static long estimateDirectBufferMb() {
            try {
                Class<?> cls = Class.forName("sun.management.ManagementFactoryHelper");
                Method method = cls.getDeclaredMethod("getBufferPools");
                method.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<java.lang.management.BufferPoolMXBean> pools =
                        (List<java.lang.management.BufferPoolMXBean>) method.invoke(null);
                return pools.stream()
                        .filter(p -> p.getName().equals("direct"))
                        .mapToLong(java.lang.management.BufferPoolMXBean::getMemoryUsed)
                        .sum() / (1024 * 1024);
            } catch (Exception e) {
                // Try the public API (Java 14+)
                try {
                    for (java.lang.management.BufferPoolMXBean pool :
                            ManagementFactory.getPlatformMXBeans(java.lang.management.BufferPoolMXBean.class)) {
                        if ("direct".equals(pool.getName())) {
                            return pool.getMemoryUsed() / (1024 * 1024);
                        }
                    }
                } catch (Exception ignored) {}
                return -1;
            }
        }
    }
}
