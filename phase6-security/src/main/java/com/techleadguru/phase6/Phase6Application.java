package com.techleadguru.phase6;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Phase 6 — Spring Security & OAuth 2.0 (Days 106–133)
 *
 * Topics:
 *   SecurityFilterChain, AuthenticationProvider, @PreAuthorize, CSRF (Days 106-112)
 *   JWT: RS256, refresh tokens, alg:none attack, revocation (Days 113-119)
 *   OAuth 2.0: Auth Code, PKCE, Client Credentials, OIDC (Days 120-126)
 *   Spring Authorization Server, token customization, multi-tenancy (Days 127-133)
 */
@SpringBootApplication
public class Phase6Application {
    public static void main(String[] args) {
        SpringApplication.run(Phase6Application.class, args);
    }
}
