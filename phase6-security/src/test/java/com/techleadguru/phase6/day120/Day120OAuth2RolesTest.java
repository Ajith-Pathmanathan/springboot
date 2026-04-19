package com.techleadguru.phase6.day120;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class Day120OAuth2RolesTest {

    @Test
    void roleDescriptions_has_4_roles() {
        assertThat(Day120OAuth2Roles.roleDescriptions()).hasSize(4);
    }

    @Test
    void roleDescriptions_covers_all_oauth2_roles() {
        var roles = Day120OAuth2Roles.roleDescriptions().stream()
                .map(Day120OAuth2Roles.RoleDescription::role)
                .toList();
        assertThat(roles).containsExactlyInAnyOrder(
                Day120OAuth2Roles.OAuth2Role.RESOURCE_OWNER,
                Day120OAuth2Roles.OAuth2Role.CLIENT,
                Day120OAuth2Roles.OAuth2Role.AUTHORIZATION_SERVER,
                Day120OAuth2Roles.OAuth2Role.RESOURCE_SERVER);
    }

    @Test
    void each_role_description_has_non_blank_fields() {
        Day120OAuth2Roles.roleDescriptions().forEach(r -> {
            assertThat(r.description()).isNotBlank();
            assertThat(r.realWorldExample()).isNotBlank();
        });
    }

    @Test
    void grantTypeGuide_has_5_grant_types() {
        assertThat(Day120OAuth2Roles.grantTypeGuide()).hasSize(5);
    }

    @Test
    void client_credentials_does_not_require_user_interaction() {
        var cc = Day120OAuth2Roles.grantTypeGuide().stream()
                .filter(g -> g.type() == Day120OAuth2Roles.GrantType.CLIENT_CREDENTIALS)
                .findFirst();
        assertThat(cc).isPresent();
        assertThat(cc.get().requiresUserInteraction()).isFalse();
    }

    @Test
    void authorization_code_pkce_requires_pkce() {
        var pkce = Day120OAuth2Roles.grantTypeGuide().stream()
                .filter(g -> g.type() == Day120OAuth2Roles.GrantType.AUTHORIZATION_CODE_PKCE)
                .findFirst();
        assertThat(pkce).isPresent();
        assertThat(pkce.get().requiresPKCE()).isTrue();
    }

    @Test
    void authorization_code_no_pkce_does_not_require_pkce() {
        var ac = Day120OAuth2Roles.grantTypeGuide().stream()
                .filter(g -> g.type() == Day120OAuth2Roles.GrantType.AUTHORIZATION_CODE)
                .findFirst();
        assertThat(ac).isPresent();
        assertThat(ac.get().requiresPKCE()).isFalse();
    }

    @Test
    void userFacingGrantTypes_does_not_include_client_credentials() {
        assertThat(Day120OAuth2Roles.userFacingGrantTypes())
                .doesNotContain(Day120OAuth2Roles.GrantType.CLIENT_CREDENTIALS);
    }

    @Test
    void pkceRequiredGrantTypes_returns_authorization_code_pkce() {
        assertThat(Day120OAuth2Roles.pkceRequiredGrantTypes())
                .contains(Day120OAuth2Roles.GrantType.AUTHORIZATION_CODE_PKCE);
    }
}
