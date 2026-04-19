package com.techleadguru.phase2.day40;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * DAY 40 — Flyway Database Migrations
 *
 * WHY DATABASE MIGRATIONS?
 *   Problem: Schema changes must be applied consistently across all environments
 *   (dev, staging, prod). Manual SQL scripts → human error, drift, forgotten steps.
 *
 *   Solution: Flyway tracks and applies SQL migrations automatically on startup.
 *   Every schema change is a versioned SQL file. Flyway never applies the same
 *   file twice. The flyway_schema_history table records what was applied.
 *
 * FLYWAY NAMING CONVENTION:
 *   V{version}__{description}.sql
 *     V = versioned migration (applied once, in order)
 *   R__{description}.sql
 *     R = repeatable migration (re-applied when checksum changes, e.g., views, functions)
 *   U{version}__{description}.sql
 *     U = undo migration (requires Flyway Teams; rolls back V migrations)
 *
 *   Examples:
 *     V1__baseline_schema.sql        — initial tables
 *     V2__add_audit_log.sql          — add audit_log table
 *     V3__add_product_category.sql   — add category column
 *     R__create_order_view.sql       — recreatable view
 *
 * FLYWAY STATES:
 *   PENDING   — migration exists but not yet applied
 *   SUCCESS   — applied successfully
 *   FAILED    — applied but threw an error (DB is in bad state)
 *   MISSING   — was in flyway_schema_history but SQL file is gone (⚠ corruption risk)
 *   IGNORED   — out of order migration when allowOutOfOrderMigration=false
 *   BASELINE  — baseline marker for existing DBs
 *
 * THE CHECKSUMS TRAP:
 *   Flyway checksums each applied SQL file. If you edit V1__baseline_schema.sql AFTER
 *   it was applied → FlywayException: "Detected resolved migration not applied to database".
 *   Fix: Never edit applied migrations. Always add NEW versioned files.
 *   Exception: Use `spring.flyway.clean-on-validation-error=true` for dev/test only.
 *
 * PRODUCTION RULES:
 *   1. NEVER edit an applied migration file.
 *   2. NEVER delete migration files.
 *   3. Add new migrations only.
 *   4. Set clean-disabled=true in production (prevent accidental clean).
 *   5. Test migrations on a staging environment before production.
 *   6. Always have a rollback plan (even if just a new migration).
 *
 * SPRING BOOT AUTO-CONFIGURATION:
 *   spring.flyway.enabled=true
 *   spring.flyway.locations=classpath:db/migration
 *   Flyway runs automatically before the app starts (before any @Transactional code).
 *   Order: Flyway runs → Hibernate validates schema → app starts.
 */
@Slf4j
public class Day40FlywayMigrations {

    @Service
    @Slf4j
    public static class MigrationInspector {

        private final Flyway flyway;

        public MigrationInspector(Flyway flyway) {
            this.flyway = flyway;
        }

        /**
         * Returns all migrations with their state.
         */
        public List<MigrationRecord> getMigrationHistory() {
            return Arrays.stream(flyway.info().all())
                    .map(info -> new MigrationRecord(
                            info.getVersion() != null ? info.getVersion().getVersion() : "repeatable",
                            info.getDescription(),
                            info.getState().name(),
                            info.getInstalledOn() != null ? info.getInstalledOn().toString() : "pending",
                            info.getChecksum() != null ? info.getChecksum() : 0
                    ))
                    .toList();
        }

        /**
         * Returns only applied migrations.
         */
        public List<MigrationRecord> getAppliedMigrations() {
            return Arrays.stream(flyway.info().applied())
                    .map(info -> new MigrationRecord(
                            info.getVersion() != null ? info.getVersion().getVersion() : "repeatable",
                            info.getDescription(),
                            info.getState().name(),
                            info.getInstalledOn() != null ? info.getInstalledOn().toString() : "pending",
                            info.getChecksum() != null ? info.getChecksum() : 0
                    ))
                    .toList();
        }

        /**
         * Returns pending migrations (not yet applied).
         */
        public List<MigrationRecord> getPendingMigrations() {
            return Arrays.stream(flyway.info().pending())
                    .map(info -> new MigrationRecord(
                            info.getVersion() != null ? info.getVersion().getVersion() : "repeatable",
                            info.getDescription(),
                            info.getState().name(),
                            "pending",
                            info.getChecksum() != null ? info.getChecksum() : 0
                    ))
                    .toList();
        }

        /**
         * Returns whether all migrations have been applied successfully.
         * If any are PENDING or FAILED, returns false — indicates schema is not current.
         */
        public boolean isSchemaUpToDate() {
            MigrationInfo[] pending = flyway.info().pending();
            MigrationInfo[] failed = Arrays.stream(flyway.info().all())
                    .filter(m -> m.getState() == MigrationState.FAILED)
                    .toArray(MigrationInfo[]::new);
            boolean upToDate = pending.length == 0 && failed.length == 0;
            log.info("[Day40] Schema up-to-date: {}, pending: {}, failed: {}",
                    upToDate, pending.length, failed.length);
            return upToDate;
        }

        /**
         * Returns the current schema version (latest applied migration).
         */
        public String getCurrentVersion() {
            MigrationInfo current = flyway.info().current();
            if (current == null) return "none";
            return current.getVersion() != null ? current.getVersion().getVersion() : "repeatable";
        }
    }

    public record MigrationRecord(
            String version,
            String description,
            String state,
            String appliedAt,
            int checksum
    ) {}
}
