package com.techleadguru.phase6.day108;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day 108 — Custom AuthenticationProvider (OTP-based).
 *
 * Spring Security lets you plug in any authentication mechanism via
 * AuthenticationProvider. This day shows an OTP provider that:
 *  - generates a 6-digit code for a username
 *  - stores it (in-memory for this demo; use Redis in production)
 *  - validates the code and returns an authenticated token on success
 *
 * Wiring (in a @Configuration class):
 *   http.authenticationProvider(new OtpAuthenticationProvider(otpService));
 */
public class Day108AuthenticationProvider {

    // ─────────────────────────────────────────────────────────────────────────
    // OtpAuthenticationToken
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Custom {@link Authentication} token for OTP-based authentication.
     * Before authentication: credentials = the OTP the user submitted.
     * After authentication: credentials are erased, authorities are set.
     */
    public static class OtpAuthenticationToken extends AbstractAuthenticationToken {

        private final String username;
        private String otp;

        /** Unauthenticated token (before validation). */
        public OtpAuthenticationToken(String username, String otp) {
            super(null);
            this.username = username;
            this.otp = otp;
            setAuthenticated(false);
        }

        /** Authenticated token (after validation). */
        public OtpAuthenticationToken(String username, Collection<? extends GrantedAuthority> authorities) {
            super(authorities);
            this.username = username;
            this.otp = null;
            setAuthenticated(true);
        }

        @Override public Object getPrincipal()   { return username; }
        @Override public Object getCredentials() { return otp; }
        @Override public void eraseCredentials() { super.eraseCredentials(); otp = null; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OtpService
    // ─────────────────────────────────────────────────────────────────────────

    /** Generates, stores and validates one-time passwords. */
    public static class OtpService {

        private final Map<String, String> store = new ConcurrentHashMap<>();
        private final SecureRandom random = new SecureRandom();

        /** Generates a 6-digit OTP for the given username and stores it. */
        public String generateOtp(String username) {
            String otp = String.format("%06d", random.nextInt(1_000_000));
            store.put(username, otp);
            return otp;
        }

        /**
         * Validates the OTP.
         * Side-effect: invalidates the OTP after one successful use.
         */
        public boolean validateOtp(String username, String otp) {
            String stored = store.get(username);
            if (stored != null && stored.equals(otp)) {
                store.remove(username);  // single-use
                return true;
            }
            return false;
        }

        /** Number of pending (unvalidated) OTPs. */
        public int pendingCount() { return store.size(); }

        /** Explicitly invalidates a pending OTP. */
        public void invalidate(String username) { store.remove(username); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OtpAuthenticationProvider
    // ─────────────────────────────────────────────────────────────────────────

    /** Spring Security {@link AuthenticationProvider} for OTP-based login. */
    public static class OtpAuthenticationProvider implements AuthenticationProvider {

        private final OtpService otpService;

        public OtpAuthenticationProvider(OtpService otpService) {
            this.otpService = otpService;
        }

        @Override
        public Authentication authenticate(Authentication authentication)
                throws AuthenticationException {
            OtpAuthenticationToken token = (OtpAuthenticationToken) authentication;
            String username = (String) token.getPrincipal();
            String otp      = (String) token.getCredentials();

            if (!otpService.validateOtp(username, otp)) {
                throw new BadCredentialsException("Invalid OTP for user: " + username);
            }

            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            OtpAuthenticationToken authenticated = new OtpAuthenticationToken(username, authorities);
            authenticated.eraseCredentials();
            return authenticated;
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return OtpAuthenticationToken.class.isAssignableFrom(authentication);
        }
    }
}
