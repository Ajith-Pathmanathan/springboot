package com.techleadguru.phase1.day08;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * DAY 8 — Test: Prove why field injection makes code untestable, constructor injection doesn't.
 *
 * KEY LEARNING: Run these tests. No Spring context needed. Runs in milliseconds.
 * These same tests on a field-injected class would need @SpringBootTest (90s startup).
 */
class Day08InjectionTest {

    // -----------------------------------------------------------------------
    // Test 1: Unit test with constructor injection — NO Spring, NO mocks framework needed
    // -----------------------------------------------------------------------
    @Test
    void constructor_injection_allows_unit_test_without_spring_context() {
        // Arrange: plain Java — no @SpringBootTest, no @MockBean, nothing
        Day08InjectionStyles.UserRepository mockRepo = id -> "MockUser[" + id + "]";
        Day08InjectionStyles.MailService mockMail = email -> {}; // no-op

        // Act: instantiate directly — this is IMPOSSIBLE with field injection
        var service = new Day08InjectionStyles.UserServiceWithConstructorInjection(mockRepo, mockMail);

        // Assert
        assertThat(service.getUser("42")).isEqualTo("MockUser[42]");
        System.out.println("[DAY 8 TEST] Unit test with constructor injection: PASSED without Spring");
        System.out.println("[DAY 8 TEST] Test took ~1ms vs ~90s for @SpringBootTest");
    }

    // -----------------------------------------------------------------------
    // Test 2: Field injection — instantiating without Spring gives NullPointerException
    // -----------------------------------------------------------------------
    @Test
    void field_injection_fails_with_NPE_when_instantiated_without_spring() {
        // Arrange: create without Spring — @Autowired fields are null
        var badService = new Day08InjectionStyles.UserServiceWithFieldInjection();

        // Act + Assert: NPE because userRepository is null
        assertThatThrownBy(() -> badService.getUser("42"))
                .isInstanceOf(NullPointerException.class);

        System.out.println("[DAY 8 TEST] Field injection = NullPointerException without Spring context");
        System.out.println("[DAY 8 TEST] This is why field injection FORCES you to use @SpringBootTest");
    }

    // -----------------------------------------------------------------------
    // Test 3: Constructor injection with Mockito — verify interactions
    // -----------------------------------------------------------------------
    @Test
    void constructor_injection_enables_interaction_verification_with_mockito() {
        // Arrange
        Day08InjectionStyles.UserRepository repo = mock(Day08InjectionStyles.UserRepository.class);
        Day08InjectionStyles.MailService mail = mock(Day08InjectionStyles.MailService.class);
        when(repo.findById("user1")).thenReturn("Alice");

        var service = new Day08InjectionStyles.UserServiceWithConstructorInjection(repo, mail);

        // Act
        String result = service.getUser("user1");
        service.registerUser("user1", "alice@example.com");

        // Assert
        assertThat(result).isEqualTo("Alice");
        verify(mail).sendWelcome("alice@example.com");
        System.out.println("[DAY 8 TEST] Mockito interactions verified. No Spring needed.");
    }

    // -----------------------------------------------------------------------
    // Test 4: Production rule documented — final fields = immutability guarantee
    // -----------------------------------------------------------------------
    @Test
    void constructor_injection_makes_dependencies_final_and_immutable() {
        // Check that the fields in the good service are final (compile-time guarantee)
        // In Java, you can verify via reflection:
        var fields = Day08InjectionStyles.UserServiceWithConstructorInjection.class.getDeclaredFields();
        long finalFieldCount = java.util.Arrays.stream(fields)
                .filter(f -> java.lang.reflect.Modifier.isFinal(f.getModifiers())
                          && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) // exclude @Slf4j 'log'
                .count();

        assertThat(finalFieldCount)
                .as("Both dependencies should be final fields — ensures immutability")
                .isEqualTo(2);

        System.out.println("[DAY 8 TEST] Final fields confirmed. Dependencies are immutable after construction.");
    }
}
