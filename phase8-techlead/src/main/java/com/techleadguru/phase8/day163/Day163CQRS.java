package com.techleadguru.phase8.day163;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day 163 — CQRS: Command Query Responsibility Segregation
 *
 * CQRS separates the write model (Commands) from the read model (Queries).
 *
 * Benefits:
 *  - Independent scaling of reads and writes
 *  - Optimise read models for query patterns (denormalised views)
 *  - Write model stays clean (aggregates / domain logic)
 */
public class Day163CQRS {

    // ─────────────────────────────────────────────────────────────────────────
    // Command model
    // ─────────────────────────────────────────────────────────────────────────

    public enum CommandType { CREATE_ORDER, UPDATE_ORDER, CANCEL_ORDER, CONFIRM_ORDER }

    public record Command(
            String      id,
            CommandType type,
            Map<String, Object> payload,
            Instant     timestamp) {

        public static Command of(CommandType type, Map<String, Object> payload) {
            return new Command(UUID.randomUUID().toString(), type, payload, Instant.now());
        }
    }

    public interface CommandHandler {
        CommandType handles();
        /** Executes the command. Returns the generated aggregate ID. */
        String handle(Command command);
    }

    public static class CreateOrderCommandHandler implements CommandHandler {

        private final Map<String, Map<String, Object>> store;

        public CreateOrderCommandHandler(Map<String, Map<String, Object>> store) {
            this.store = store;
        }

        @Override public CommandType handles() { return CommandType.CREATE_ORDER; }

        @Override
        public String handle(Command command) {
            String orderId = UUID.randomUUID().toString();
            Map<String, Object> data = new HashMap<>(command.payload());
            data.put("orderId", orderId);
            data.put("status",  "PENDING");
            store.put(orderId, data);
            return orderId;
        }
    }

    public static class UpdateOrderCommandHandler implements CommandHandler {

        private final Map<String, Map<String, Object>> store;

        public UpdateOrderCommandHandler(Map<String, Map<String, Object>> store) {
            this.store = store;
        }

        @Override public CommandType handles() { return CommandType.UPDATE_ORDER; }

        @Override
        public String handle(Command command) {
            String orderId = (String) command.payload().get("orderId");
            Map<String, Object> existing = store.get(orderId);
            if (existing == null) throw new NoSuchElementException("Order not found: " + orderId);
            existing.putAll(command.payload());
            return orderId;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Command bus
    // ─────────────────────────────────────────────────────────────────────────

    public static class CommandBus {

        private final Map<CommandType, CommandHandler> handlers = new EnumMap<>(CommandType.class);

        public CommandBus register(CommandHandler handler) {
            handlers.put(handler.handles(), handler);
            return this;
        }

        public String dispatch(Command command) {
            CommandHandler h = handlers.get(command.type());
            if (h == null) throw new IllegalStateException("No handler for " + command.type());
            return h.handle(command);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Query (read) model
    // ─────────────────────────────────────────────────────────────────────────

    public record OrderQueryModel(
            String  orderId,
            String  status,
            double  total,
            String  customerName,
            Instant lastUpdated) {}

    public static class OrderQueryStore {

        private final Map<String, OrderQueryModel> store = new ConcurrentHashMap<>();

        public void save(OrderQueryModel model) { store.put(model.orderId(), model); }

        public Optional<OrderQueryModel> findById(String orderId) {
            return Optional.ofNullable(store.get(orderId));
        }

        public List<OrderQueryModel> findByStatus(String status) {
            return store.values().stream()
                        .filter(m -> m.status().equals(status))
                        .toList();
        }

        public List<OrderQueryModel> findAll() { return List.copyOf(store.values()); }

        public int size() { return store.size(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Benefits guide
    // ─────────────────────────────────────────────────────────────────────────

    public record CqrsBenefit(String area, String description) {}

    public static List<CqrsBenefit> cqrsBenefits() {
        return List.of(
            new CqrsBenefit("Scalability",
                "Read replicas can scale independently from the write database"),
            new CqrsBenefit("Performance",
                "Read models are denormalised and optimised for specific queries"),
            new CqrsBenefit("Domain clarity",
                "Write model stays rich and clean; reads are dumb projections"),
            new CqrsBenefit("Evolvability",
                "Read models can be rebuilt from events at any time"),
            new CqrsBenefit("Flexibility",
                "Different storage engines per side (e.g., Postgres write + Elasticsearch read)")
        );
    }
}
