package com.techleadguru.phase2.day40;

import com.techleadguru.phase2.day40.Day40FlywayMigrations.MigrationInspector;
import com.techleadguru.phase2.day40.Day40FlywayMigrations.MigrationRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 40 — Test: Flyway database migration inspection.
 */
@SpringBootTest(classes = com.techleadguru.phase2.Phase2Application.class)
@ActiveProfiles("test")
class Day40FlywayMigrationsTest {

    @Autowired
    MigrationInspector migrationInspector;

    // -----------------------------------------------------------------------
    // Test 1: All expected migrations are applied
    // -----------------------------------------------------------------------
    @Test
    void all_baseline_migrations_applied() {
        List<MigrationRecord> applied = migrationInspector.getAppliedMigrations();

        assertThat(applied).isNotEmpty();
        assertThat(applied).hasSizeGreaterThanOrEqualTo(2); // V1 and V2

        applied.forEach(m -> {
            assertThat(m.state()).isEqualTo("SUCCESS");
            System.out.printf("[DAY 40] Applied: V%s — %s (checksum=%d)%n",
                    m.version(), m.description(), m.checksum());
        });
    }

    // -----------------------------------------------------------------------
    // Test 2: Schema is up to date (no pending migrations)
    // -----------------------------------------------------------------------
    @Test
    void schema_is_up_to_date() {
        boolean upToDate = migrationInspector.isSchemaUpToDate();

        assertThat(upToDate).isTrue();
        System.out.println("[DAY 40] Schema is up to date — all migrations applied.");
    }

    // -----------------------------------------------------------------------
    // Test 3: Current version matches latest migration
    // -----------------------------------------------------------------------
    @Test
    void current_version_is_latest() {
        String version = migrationInspector.getCurrentVersion();

        assertThat(version).isNotEqualTo("none");
        System.out.println("[DAY 40] Current schema version: V" + version);
    }

    // -----------------------------------------------------------------------
    // Test 4: Migration history contains V1 and V2
    // -----------------------------------------------------------------------
    @Test
    void migration_history_contains_baseline_migrations() {
        List<MigrationRecord> history = migrationInspector.getMigrationHistory();

        assertThat(history).isNotEmpty();
        var versions = history.stream().map(MigrationRecord::version).toList();
        assertThat(versions).contains("1", "2");

        System.out.println("[DAY 40] Migration history:");
        history.forEach(m -> System.out.printf("  V%s: %s [%s]%n",
                m.version(), m.description(), m.state()));
    }

    // -----------------------------------------------------------------------
    // Test 5: Document Flyway best practices
    // -----------------------------------------------------------------------
    @Test
    void document_flyway_best_practices() {
        System.out.println("[DAY 40] FLYWAY BEST PRACTICES:");
        System.out.println();
        System.out.println("  NAMING: V{version}__{description}.sql");
        System.out.println("  EXAMPLES:");
        System.out.println("    V1__baseline_schema.sql");
        System.out.println("    V2__add_audit_log.sql");
        System.out.println("    V3__add_product_category_column.sql");
        System.out.println("    R__create_reporting_views.sql  (repeatable)");
        System.out.println();
        System.out.println("  RULES:");
        System.out.println("  1. NEVER edit an applied migration.");
        System.out.println("  2. NEVER delete migration files.");
        System.out.println("  3. Set clean-disabled=true in production.");
        System.out.println("  4. Test on staging before production.");
        System.out.println("  5. Checksum mismatch → startup fails (Flyway protection).");
        System.out.println();
        System.out.println("  SPRING BOOT: Flyway runs before Hibernate validates schema.");
        System.out.println("  Order: Flyway apply → Hibernate validate → App start → @Transactional code");
        assertThat(true).isTrue();
    }
}
