package com.techleadguru.phase7.day149;

import java.util.*;

/**
 * Day 149 — Spring Cloud Stream with Kafka binder
 *
 * Spring Cloud Stream abstracts the messaging middleware behind:
 *   - Function-based programming model (java.util.function.Function/Consumer/Supplier)
 *   - Binder abstraction (Kafka, RabbitMQ, Pulsar)
 *   - Binding configuration via spring.cloud.stream.bindings.*
 *
 * Spring Cloud Stream 3.x moved from @EnableBinding + @StreamListener
 * to plain java.util.function beans.
 */
public class Day149CloudStream {

    // ─────────────────────────────────────────────────────────────────────────
    // Binding model
    // ─────────────────────────────────────────────────────────────────────────

    public record StreamBinding(
            String bindingName,         // e.g. "process-in-0" or "process-out-0"
            String destination,         // Kafka topic or exchange
            String group,               // consumer group (null for producers)
            String binder) {}           // "kafka" or "rabbit"

    public record FunctionBinding(
            String functionName,        // Spring bean name of the java.util.function.*
            String kind,                // "Consumer", "Supplier", "Function"
            String inputBinding,        // auto-generated: {fn}-in-0
            String outputBinding) {}    // auto-generated: {fn}-out-0 (null for Consumer)

    // ─────────────────────────────────────────────────────────────────────────
    // Message envelope
    // ─────────────────────────────────────────────────────────────────────────

    public record MessageEnvelope(
            Object              payload,
            Map<String, Object> headers) {

        public static MessageEnvelope of(Object payload) {
            return new MessageEnvelope(payload, new LinkedHashMap<>());
        }

        public static MessageEnvelope of(Object payload, Map<String, Object> headers) {
            return new MessageEnvelope(payload, Collections.unmodifiableMap(headers));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Configuration properties builder
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns application.properties entries for a function binding.
     * For a Consumer named "orderProcessor" consuming from topic "orders":
     *   functionName="orderProcessor", topic="orders", group="order-svc"
     */
    public static Map<String, String> bindingProperties(
            String functionName, String topic, String group) {
        Map<String, String> props = new LinkedHashMap<>();
        String in  = functionName + "-in-0";
        String out = functionName + "-out-0";
        props.put("spring.cloud.function.definition",                               functionName);
        props.put("spring.cloud.stream.bindings." + in + ".destination",            topic);
        props.put("spring.cloud.stream.bindings." + in + ".group",                  group);
        props.put("spring.cloud.stream.bindings." + out + ".destination",           topic + ".output");
        props.put("spring.cloud.stream.kafka.binder.brokers",                       "localhost:9092");
        props.put("spring.cloud.stream.kafka.bindings." + in + ".consumer.start-offset", "earliest");
        return props;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Migration guide from annotation-based style
    // ─────────────────────────────────────────────────────────────────────────

    public record MigrationStep(int order, String oldStyle, String newStyle) {}

    public static List<MigrationStep> migrationFromAnnotationStyle() {
        return List.of(
            new MigrationStep(1,
                "@EnableBinding(Sink.class) on @SpringBootApplication",
                "Remove @EnableBinding; use spring.cloud.function.definition property"),
            new MigrationStep(2,
                "@StreamListener(Sink.INPUT) void handle(Message msg)",
                "@Bean Consumer<Message<String>> handle() { return msg -> ...; }"),
            new MigrationStep(3,
                "@Output(Source.OUTPUT) MessageChannel output; output.send(msg)",
                "@Bean Supplier<String> source() { return () -> ...; }"),
            new MigrationStep(4,
                "@StreamListener + @SendTo for transform",
                "@Bean Function<String,String> transform() { return s -> ...; }"),
            new MigrationStep(5,
                "Processor interface for in+out",
                "Function<String,String> with input and output bindings auto-resolved")
        );
    }
}
