package com.techleadguru.phase7.day159;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day 159 — Structured logging with Logback and MDC
 *
 * Structured logging emits log lines as JSON so that log aggregators
 * (ELK, Loki, Datadog) can index fields and enable powerful queries.
 *
 * MDC (Mapped Diagnostic Context) stores per-thread metadata (traceId, userId)
 * that Logback automatically includes in each log statement on that thread.
 *
 * Spring Boot 3 includes Logback as the default logging implementation.
 * logstash-logback-encoder converts all log events to JSON automatically.
 */
public class Day159StructuredLogging {

    // ─────────────────────────────────────────────────────────────────────────
    // Log entry model
    // ─────────────────────────────────────────────────────────────────────────

    public record LogEntry(
            String              timestamp,   // ISO-8601
            String              level,       // INFO / WARN / ERROR
            String              traceId,
            String              spanId,
            String              service,
            String              logger,
            String              message,
            Map<String, Object> extra) {}    // additional structured fields

    // ─────────────────────────────────────────────────────────────────────────
    // MDC context (thin ThreadLocal map wrapper)
    // ─────────────────────────────────────────────────────────────────────────

    public static class MdcContext {

        private static final ThreadLocal<Map<String, String>> context =
                ThreadLocal.withInitial(LinkedHashMap::new);

        public static void put(String key, String value) {
            context.get().put(key, value);
        }

        public static String get(String key) {
            return context.get().get(key);
        }

        public static void remove(String key) {
            context.get().remove(key);
        }

        public static Map<String, String> getCopyOfContextMap() {
            return new LinkedHashMap<>(context.get());
        }

        public static void clear() {
            context.remove();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON logger (for testing / demo — uses MdcContext)
    // ─────────────────────────────────────────────────────────────────────────

    public static class JsonLogger {

        private final String serviceName;
        private final String loggerName;
        private final List<LogEntry> capturedEntries = new ArrayList<>();

        public JsonLogger(String serviceName, String loggerName) {
            this.serviceName = serviceName;
            this.loggerName  = loggerName;
        }

        public void log(String level, String message, Map<String, Object> extra) {
            Map<String, String> mdc = MdcContext.getCopyOfContextMap();
            LogEntry entry = new LogEntry(
                    Instant.now().toString(),
                    level,
                    mdc.getOrDefault("traceId", ""),
                    mdc.getOrDefault("spanId", ""),
                    serviceName,
                    loggerName,
                    message,
                    extra != null ? extra : Map.of()
            );
            capturedEntries.add(entry);
        }

        public void info(String msg)  { log("INFO",  msg, null); }
        public void warn(String msg)  { log("WARN",  msg, null); }
        public void error(String msg) { log("ERROR", msg, null); }

        public String formatAsJson(LogEntry e) {
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"timestamp\":\"").append(e.timestamp()).append("\",");
            sb.append("\"level\":\"").append(e.level()).append("\",");
            sb.append("\"traceId\":\"").append(e.traceId()).append("\",");
            sb.append("\"spanId\":\"").append(e.spanId()).append("\",");
            sb.append("\"service\":\"").append(e.service()).append("\",");
            sb.append("\"logger\":\"").append(e.logger()).append("\",");
            sb.append("\"message\":\"").append(e.message()).append("\"");
            e.extra().forEach((k, v) ->
                sb.append(",\"").append(k).append("\":\"").append(v).append("\""));
            sb.append("}");
            return sb.toString();
        }

        public List<LogEntry> capturedEntries() {
            return Collections.unmodifiableList(capturedEntries);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Standard MDC field guide
    // ─────────────────────────────────────────────────────────────────────────

    public record MdcField(String key, String populatedBy, String description) {}

    public static List<MdcField> logFields() {
        return List.of(
            new MdcField("traceId",     "Micrometer Tracing filter",  "Distributed trace identifier"),
            new MdcField("spanId",      "Micrometer Tracing filter",  "Current span within the trace"),
            new MdcField("userId",      "Security/auth filter",       "Authenticated user identity"),
            new MdcField("tenantId",    "Tenant resolver filter",     "Multi-tenancy context"),
            new MdcField("requestId",   "Request ID filter",          "Unique per HTTP request"),
            new MdcField("sessionId",   "Session filter",             "HTTP session identifier")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Logging properties
    // ─────────────────────────────────────────────────────────────────────────

    public static Map<String, String> structuredLoggingProperties() {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("logging.structured.format.console", "ecs");
        props.put("logging.pattern.level",
                  "%5p [${spring.application.name},%X{traceId:-},%X{spanId:-}]");
        props.put("logging.level.root",                "INFO");
        props.put("logging.level.com.techleadguru",    "DEBUG");
        return props;
    }
}
