package com.techleadguru.phase6.day111;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class Day111PreAuthorizeTest {

    @Test
    void commonExpressions_returns_at_least_8_examples() {
        assertThat(Day111PreAuthorize.commonExpressions()).hasSizeGreaterThanOrEqualTo(8);
    }

    @Test
    void each_example_has_non_blank_fields() {
        Day111PreAuthorize.commonExpressions().forEach(ex -> {
            assertThat(ex.expression()).isNotBlank();
            assertThat(ex.description()).isNotBlank();
            assertThat(ex.exampleUseCase()).isNotBlank();
        });
    }

    @Test
    void hasRole_builds_correct_expression() {
        assertThat(Day111PreAuthorize.hasRole("ADMIN")).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void hasAnyRole_single_role() {
        assertThat(Day111PreAuthorize.hasAnyRole("USER")).isEqualTo("hasAnyRole('USER')");
    }

    @Test
    void hasAnyRole_multiple_roles() {
        String expr = Day111PreAuthorize.hasAnyRole("USER", "ADMIN");
        assertThat(expr).startsWith("hasAnyRole(");
        assertThat(expr).contains("'USER'");
        assertThat(expr).contains("'ADMIN'");
    }

    @Test
    void hasAuthority_builds_correct_expression() {
        assertThat(Day111PreAuthorize.hasAuthority("SCOPE_read:orders"))
                .isEqualTo("hasAuthority('SCOPE_read:orders')");
    }

    @Test
    void paramMatchesCurrentUser_builds_principal_check() {
        String expr = Day111PreAuthorize.paramMatchesCurrentUser("userId");
        assertThat(expr).isEqualTo("#userId == authentication.name");
    }

    @Test
    void beanMethod_builds_bean_check_expression() {
        String expr = Day111PreAuthorize.beanMethod("ownerBean", "isOwner", "resourceId");
        assertThat(expr).isEqualTo("@ownerBean.isOwner(#resourceId, authentication.name)");
    }

    @Test
    void commonPitfalls_has_4_entries() {
        assertThat(Day111PreAuthorize.commonPitfalls()).hasSize(4);
    }

    @Test
    void commonPitfalls_mentions_private_method() {
        boolean hasPrivate = Day111PreAuthorize.commonPitfalls().keySet().stream()
                .anyMatch(k -> k.toLowerCase().contains("private"));
        assertThat(hasPrivate).isTrue();
    }

    @Test
    void commonPitfalls_mentions_enable_method_security() {
        boolean hasEnableMethodSecurity = Day111PreAuthorize.commonPitfalls().keySet().stream()
                .anyMatch(k -> k.contains("@EnableMethodSecurity"));
        assertThat(hasEnableMethodSecurity).isTrue();
    }
}
