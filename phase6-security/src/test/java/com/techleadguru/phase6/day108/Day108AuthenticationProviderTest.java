package com.techleadguru.phase6.day108;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import static org.assertj.core.api.Assertions.*;

class Day108AuthenticationProviderTest {

    private Day108AuthenticationProvider.OtpService      otpService;
    private Day108AuthenticationProvider.OtpAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        otpService = new Day108AuthenticationProvider.OtpService();
        provider   = new Day108AuthenticationProvider.OtpAuthenticationProvider(otpService);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OtpService
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void generateOtp_returns_6_digit_string() {
        String otp = otpService.generateOtp("alice");
        assertThat(otp).hasSize(6).matches("\\d{6}");
    }

    @Test
    void generateOtp_increments_pending_count() {
        assertThat(otpService.pendingCount()).isZero();
        otpService.generateOtp("alice");
        assertThat(otpService.pendingCount()).isEqualTo(1);
    }

    @Test
    void validateOtp_returns_true_for_correct_otp() {
        String otp = otpService.generateOtp("alice");
        assertThat(otpService.validateOtp("alice", otp)).isTrue();
    }

    @Test
    void validateOtp_returns_false_for_wrong_otp() {
        otpService.generateOtp("alice");
        assertThat(otpService.validateOtp("alice", "000000")).isFalse();
    }

    @Test
    void validateOtp_is_single_use() {
        String otp = otpService.generateOtp("alice");
        assertThat(otpService.validateOtp("alice", otp)).isTrue();
        // Second attempt with same OTP must fail
        assertThat(otpService.validateOtp("alice", otp)).isFalse();
    }

    @Test
    void validateOtp_decrements_pending_after_success() {
        String otp = otpService.generateOtp("alice");
        otpService.validateOtp("alice", otp);
        assertThat(otpService.pendingCount()).isZero();
    }

    @Test
    void invalidate_removes_pending_otp() {
        otpService.generateOtp("alice");
        otpService.invalidate("alice");
        assertThat(otpService.pendingCount()).isZero();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OtpAuthenticationToken
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void unauthenticated_token_is_not_authenticated() {
        var token = new Day108AuthenticationProvider.OtpAuthenticationToken("alice", "123456");
        assertThat(token.isAuthenticated()).isFalse();
        assertThat(token.getPrincipal()).isEqualTo("alice");
        assertThat(token.getCredentials()).isEqualTo("123456");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OtpAuthenticationProvider
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void provider_supports_otp_token() {
        assertThat(provider.supports(Day108AuthenticationProvider.OtpAuthenticationToken.class))
                .isTrue();
    }

    @Test
    void authenticate_succeeds_with_correct_otp() {
        String otp  = otpService.generateOtp("alice");
        var    token = new Day108AuthenticationProvider.OtpAuthenticationToken("alice", otp);
        var    result = provider.authenticate(token);
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isEqualTo("alice");
        assertThat(result.getAuthorities()).isNotEmpty();
    }

    @Test
    void authenticate_throws_bad_credentials_for_wrong_otp() {
        otpService.generateOtp("alice");
        var token = new Day108AuthenticationProvider.OtpAuthenticationToken("alice", "wrong");
        assertThatThrownBy(() -> provider.authenticate(token))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("alice");
    }

    @Test
    void authenticated_token_has_role_user() {
        String otp    = otpService.generateOtp("alice");
        var    token  = new Day108AuthenticationProvider.OtpAuthenticationToken("alice", otp);
        var    result = provider.authenticate(token);
        assertThat(result.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }
}
