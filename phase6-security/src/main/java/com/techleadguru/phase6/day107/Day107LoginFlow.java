package com.techleadguru.phase6.day107;

import java.util.List;
import java.util.Map;

/**
 * Day 107 — Login request flow: Filter → AuthenticationManager → SecurityContext.
 *
 * When a user submits POST /login, Spring Security processes it like this:
 *
 *  1. UsernamePasswordAuthenticationFilter matches the request
 *  2. Extracts username/password from the request
 *  3. Creates an unauthenticated UsernamePasswordAuthenticationToken
 *  4. Delegates to AuthenticationManager (usually ProviderManager)
 *  5. ProviderManager iterates its AuthenticationProvider list
 *  6. DaoAuthenticationProvider loads UserDetails and checks password
 *  7. On success: authenticated token stored in SecurityContextHolder
 *  8. SimpleUrlAuthenticationSuccessHandler redirects to /
 *  9. On failure: SimpleUrlAuthenticationFailureHandler redirects to /login?error
 */
public class Day107LoginFlow {

    /** One step in the Spring Security authentication flow. */
    public record LoginStep(int order, String component, String action, String detail) {}

    /** Returns the full UsernamePasswordAuthentication flow in order. */
    public static List<LoginStep> usernamePasswordLoginFlow() {
        return List.of(
            new LoginStep(1, "UsernamePasswordAuthenticationFilter",
                "Matches request",
                "Checks method=POST and requestURL matches /login"),
            new LoginStep(2, "UsernamePasswordAuthenticationFilter",
                "Extracts credentials",
                "Reads username & password from request parameters"),
            new LoginStep(3, "UsernamePasswordAuthenticationFilter",
                "Creates unauthenticated token",
                "new UsernamePasswordAuthenticationToken(username, password)"),
            new LoginStep(4, "ProviderManager (AuthenticationManager)",
                "Delegates to providers",
                "Iterates registered AuthenticationProvider instances"),
            new LoginStep(5, "DaoAuthenticationProvider",
                "Loads user details",
                "UserDetailsService.loadUserByUsername(username)"),
            new LoginStep(6, "DaoAuthenticationProvider",
                "Validates password",
                "PasswordEncoder.matches(rawPassword, encodedPassword)"),
            new LoginStep(7, "DaoAuthenticationProvider",
                "Returns authenticated token",
                "new UsernamePasswordAuthenticationToken(userDetails, null, authorities)"),
            new LoginStep(8, "SecurityContextHolderFilter",
                "Stores in context",
                "SecurityContextHolder.getContext().setAuthentication(token)"),
            new LoginStep(9, "SavedRequestAwareAuthenticationSuccessHandler",
                "Redirects on success",
                "Redirects to saved request URL or default /"),
            new LoginStep(10, "SimpleUrlAuthenticationFailureHandler",
                "Redirects on failure",
                "Redirects to /login?error if credentials are wrong")
        );
    }

    /** Returns the key components and their roles in the auth infrastructure. */
    public static Map<String, String> authenticationManagerComponents() {
        return Map.of(
            "AuthenticationManager",
            "Entry point; takes an unauthenticated token, returns an authenticated one",
            "ProviderManager",
            "Main implementation; holds a list of AuthenticationProvider instances",
            "AuthenticationProvider",
            "Strategy for one authentication method (password, OTP, LDAP, etc.)",
            "UserDetailsService",
            "Loads UserDetails (username, password hash, authorities) from storage",
            "PasswordEncoder",
            "Hashes and verifies passwords; BCryptPasswordEncoder recommended",
            "SecurityContextHolder",
            "Thread-local store for the current user's Authentication object"
        );
    }

    /** Returns the HTTP Bearer token (JWT) flow steps (OAuth2 Resource Server). */
    public static List<LoginStep> bearerTokenFlow() {
        return List.of(
            new LoginStep(1, "BearerTokenAuthenticationFilter",
                "Extracts Bearer token",
                "Reads 'Authorization: Bearer <token>' header"),
            new LoginStep(2, "BearerTokenAuthenticationFilter",
                "Creates unauthenticated token",
                "new BearerTokenAuthenticationToken(tokenValue)"),
            new LoginStep(3, "ProviderManager",
                "Delegates to JwtAuthenticationProvider",
                "JwtAuthenticationProvider handles BearerTokenAuthenticationToken"),
            new LoginStep(4, "JwtDecoder",
                "Validates JWT",
                "Signature check, expiry check, issuer check"),
            new LoginStep(5, "JwtAuthenticationConverter",
                "Converts JWT claims to authorities",
                "Reads 'scope' or 'roles' claim from JWT"),
            new LoginStep(6, "SecurityContextHolder",
                "Stores JwtAuthenticationToken",
                "Principal = Jwt object with all claims")
        );
    }
}
