package com.techleadguru.phase3.day50;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DAY 50 — Test: Custom ConstraintValidator enforces domain rules on @Valid @RequestBody.
 */
@SpringBootTest(classes = com.techleadguru.phase3.Phase3Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Day50BeanValidationTest {

    @Autowired
    MockMvc mockMvc;

    private static final String VALID_REQUEST =
            "{\"username\":\"alice_99\",\"email\":\"alice@example.com\",\"password\":\"Str0ng!Pass\",\"age\":25}";

    // -----------------------------------------------------------------------
    // Test 1: Valid request passes all validators → 201
    // -----------------------------------------------------------------------
    @Test
    void valid_registration_returns_201() throws Exception {
        mockMvc.perform(post("/api/day50/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice_99"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        System.out.println("[DAY 50] All validators passed → 201 Created");
    }

    // -----------------------------------------------------------------------
    // Test 2: Weak password → @StrongPassword constraint fails
    // -----------------------------------------------------------------------
    @Test
    void weak_password_fails_custom_validator() throws Exception {
        String request = "{\"username\":\"bob\",\"email\":\"bob@test.com\",\"password\":\"weak\",\"age\":30}";

        mockMvc.perform(post("/api/day50/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));

        System.out.println("[DAY 50] @StrongPassword rejected 'weak' — no uppercase, digit, or special char");
    }

    // -----------------------------------------------------------------------
    // Test 3: Invalid email → @Email constraint fails
    // -----------------------------------------------------------------------
    @Test
    void invalid_email_returns_400() throws Exception {
        String request = "{\"username\":\"charlie\",\"email\":\"not-an-email\",\"password\":\"Str0ng!Pass\",\"age\":25}";

        mockMvc.perform(post("/api/day50/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        System.out.println("[DAY 50] @Email rejected 'not-an-email'");
    }

    // -----------------------------------------------------------------------
    // Test 4: Invalid username → @ValidUsername fails (too short / invalid chars)
    // -----------------------------------------------------------------------
    @Test
    void invalid_username_fails_custom_validator() throws Exception {
        String shortUsername =
                "{\"username\":\"ab\",\"email\":\"ok@test.com\",\"password\":\"Str0ng!Pass\",\"age\":25}";

        mockMvc.perform(post("/api/day50/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shortUsername))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("username"));

        String specialChars =
                "{\"username\":\"bad name!\",\"email\":\"ok@test.com\",\"password\":\"Str0ng!Pass\",\"age\":25}";
        mockMvc.perform(post("/api/day50/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(specialChars))
                .andExpect(status().isBadRequest());

        System.out.println("[DAY 50] @ValidUsername rejected 'ab' (too short) and 'bad name!'");
    }

    // -----------------------------------------------------------------------
    // Test 5: Multiple validation violations returned together
    // -----------------------------------------------------------------------
    @Test
    void multiple_violations_returned_in_single_response() throws Exception {
        String allBad = "{\"username\":\"x\",\"email\":\"not-email\",\"password\":\"short\",\"age\":-1}";

        String body = mockMvc.perform(post("/api/day50/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(allBad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("username");
        assertThat(body).contains("email");
        assertThat(body).contains("password");

        System.out.println("[DAY 50] Multiple violations returned together — fail-fast=false by default");
        System.out.println("[DAY 50] Body: " + body);
    }

    // -----------------------------------------------------------------------
    // Test 6: Document bean validation
    // -----------------------------------------------------------------------
    @Test
    void document_custom_constraint_validator() {
        System.out.println("[DAY 50] CUSTOM ConstraintValidator RECIPE:");
        System.out.println();
        System.out.println("  1. @interface @StrongPassword {");
        System.out.println("       @Constraint(validatedBy = StrongPasswordValidator.class)");
        System.out.println("     }");
        System.out.println("  2. class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {");
        System.out.println("       isValid(value, context) → return true/false");
        System.out.println("     }");
        System.out.println("  3. Use like any built-in: @StrongPassword String password");
        System.out.println();
        System.out.println("  ERROR HANDLING:");
        System.out.println("    @Valid failure → MethodArgumentNotValidException");
        System.out.println("    Day49 @ControllerAdvice handles it → 400 ProblemDetail with fieldErrors");
        assertThat(true).isTrue();
    }
}
