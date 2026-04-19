package com.techleadguru.phase2.day39;

import com.techleadguru.phase2.day39.Day39ReadReplicaRouting.OrderReadService;
import com.techleadguru.phase2.day39.Day39ReadReplicaRouting.RoutingDataSourceContext;
import com.techleadguru.phase2.day39.Day39ReadReplicaRouting.RoutingDataSourceContext.DataSourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 39 — Test: Read replica routing with AbstractRoutingDataSource.
 *
 * NOTE: In this demo, both primary and replica use the SAME H2 in-memory DB.
 * The routing logic and context mechanism is what's being demonstrated, not
 * separate physical databases.
 */
@SpringBootTest(classes = com.techleadguru.phase2.Phase2Application.class)
@ActiveProfiles("test")
class Day39ReadReplicaRoutingTest {

    @Autowired
    OrderReadService orderReadService;

    @AfterEach
    void clearContext() {
        RoutingDataSourceContext.clear();
    }

    // -----------------------------------------------------------------------
    // Test 1: Default routing goes to PRIMARY
    // -----------------------------------------------------------------------
    @Test
    void default_routing_goes_to_primary() {
        RoutingDataSourceContext.clear(); // no type set

        String currentType = orderReadService.getCurrentDataSourceType();
        assertThat(currentType).contains("PRIMARY");

        System.out.println("[DAY 39] Default routing: " + currentType);
        System.out.println("[DAY 39] No context set → PRIMARY is the safe default.");
    }

    // -----------------------------------------------------------------------
    // Test 2: Setting REPLICA context routes to replica
    // -----------------------------------------------------------------------
    @Test
    void explicit_replica_context_routes_to_replica() {
        RoutingDataSourceContext.setDataSourceType(DataSourceType.REPLICA);

        String currentType = orderReadService.getCurrentDataSourceType();
        assertThat(currentType).isEqualTo("REPLICA");

        System.out.println("[DAY 39] Replica context set → routing to: " + currentType);
    }

    // -----------------------------------------------------------------------
    // Test 3: Write operation routes to PRIMARY
    // -----------------------------------------------------------------------
    @Test
    void write_operation_goes_to_primary() {
        String result = orderReadService.writeToPrimary("order-data-123");

        assertThat(result).startsWith("PRIMARY:");
        System.out.println("[DAY 39] Write routed to: " + result);
    }

    // -----------------------------------------------------------------------
    // Test 4: Read operation (readOnly=true) can use REPLICA
    // -----------------------------------------------------------------------
    @Test
    void read_operation_can_route_to_replica() {
        String result = orderReadService.readFromReplica("order-data-456");

        assertThat(result).startsWith("REPLICA:");
        System.out.println("[DAY 39] Read routed to: " + result);
    }

    // -----------------------------------------------------------------------
    // Test 5: Document read replica architecture
    // -----------------------------------------------------------------------
    @Test
    void document_read_replica_architecture() {
        System.out.println("[DAY 39] READ REPLICA ROUTING:");
        System.out.println();
        System.out.println("  AbstractRoutingDataSource wraps multiple DataSources.");
        System.out.println("  determineCurrentLookupKey() selects which DataSource to use.");
        System.out.println();
        System.out.println("  ROUTING STRATEGY:");
        System.out.println("  @Transactional(readOnly=true) → REPLICA (AOP sets REPLICA context)");
        System.out.println("  @Transactional               → PRIMARY (writes must go to master)");
        System.out.println("  No @Transactional            → PRIMARY (safe default)");
        System.out.println();
        System.out.println("  PRODUCTION CONSIDERATIONS:");
        System.out.println("  - Replication lag: replica may be behind by 100ms-1s.");
        System.out.println("  - Never read your own write from replica immediately.");
        System.out.println("  - Health check: failover to PRIMARY if replica down.");
        System.out.println("  - Multiple replicas: round-robin for load distribution.");
        assertThat(true).isTrue();
    }
}
