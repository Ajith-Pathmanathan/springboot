package com.techleadguru.phase3.day50;

import jakarta.validation.*;
import jakarta.validation.constraints.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.*;
import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * DAY 50 — Bean Validation: Custom ConstraintValidator
 *
 * JSR-380 (Bean Validation 3.0) is integrated into Spring MVC via:
 *   @Valid / @Validated → MethodValidationInterceptor → ConstraintValidator chain
 *
 * BUILT-IN CONSTRAINTS (jakarta.validation.constraints):
 *   @NotNull, @NotBlank, @NotEmpty — null/blank checks
 *   @Size(min, max)               — collection/string size
 *   @Min, @Max, @Positive, @Negative — numeric range
 *   @Email                        — basic email format check
 *   @Pattern(regexp)              — regex validation
 *   @Past, @Future                — date constraints
 *
 * CUSTOM ConstraintValidator — for domain-specific rules:
 *   1. @interface Annotation with @Constraint(validatedBy = MyValidator.class)
 *   2. class MyValidator implements ConstraintValidator<MyAnnotation, ValueType>
 *      - initialize(annotation) — read annotation parameters
 *      - isValid(value, context) — your validation logic
 *      - Set custom message via context.buildConstraintViolationWithTemplate()
 *
 * @Valid vs @Validated:
 *   @Valid    — JSR-380 standard, works everywhere
 *   @Validated — Spring extension: supports groups + method-level validation on any bean
 *
 * WHERE VALIDATION FIRES:
 *   @RequestBody @Valid CreateDto dto  → validates JSON payload
 *   @PathVariable @Positive int id     → validates URL path variable (needs @Validated on class)
 *   @RequestParam @NotBlank String q   → validates query param  (needs @Validated on class)
 */
@Slf4j
public class Day50BeanValidation {

    // =========================================================================
    // Custom constraint: @StrongPassword
    // At least 8 chars, 1 uppercase, 1 digit, 1 special character
    // =========================================================================

    @Documented
    @Constraint(validatedBy = StrongPasswordValidator.class)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface StrongPassword {
        String message() default "Password must be ≥8 chars with uppercase, digit, and special character";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    public static class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

        private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
        private static final Pattern DIGIT     = Pattern.compile("[0-9]");
        private static final Pattern SPECIAL   = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

        @Override
        public boolean isValid(String password, ConstraintValidatorContext ctx) {
            if (password == null || password.length() < 8) {
                return false;
            }
            boolean hasUpper   = UPPERCASE.matcher(password).find();
            boolean hasDigit   = DIGIT.matcher(password).find();
            boolean hasSpecial = SPECIAL.matcher(password).find();

            if (!hasUpper || !hasDigit || !hasSpecial) {
                ctx.disableDefaultConstraintViolation();
                ctx.buildConstraintViolationWithTemplate(
                        String.format("Password must have: uppercase=%s, digit=%s, special=%s",
                                hasUpper, hasDigit, hasSpecial)
                ).addConstraintViolation();
                return false;
            }
            return true;
        }
    }

    // =========================================================================
    // Custom constraint: @ValidUsername — alphanumeric + underscore, 3-20 chars
    // =========================================================================

    @Documented
    @Constraint(validatedBy = UsernameValidator.class)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ValidUsername {
        String message() default "Username must be 3-20 chars, alphanumeric and underscores only";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    public static class UsernameValidator implements ConstraintValidator<ValidUsername, String> {
        private static final Pattern VALID = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

        @Override
        public boolean isValid(String username, ConstraintValidatorContext ctx) {
            if (username == null) return false;
            return VALID.matcher(username).matches();
        }
    }

    // =========================================================================
    // Request DTO — uses built-in + custom constraints
    // =========================================================================

    public record RegisterRequest(
            @ValidUsername
            String username,

            @NotBlank(message = "Email is required")
            @Email(message = "Must be a valid email address")
            String email,

            @StrongPassword
            String password,

            @NotNull
            @Positive(message = "Age must be positive")
            @Max(value = 130, message = "Age must be ≤ 130")
            Integer age
    ) {}

    public record RegisterResponse(String username, String email, String message) {}

    // =========================================================================
    // Controller
    // =========================================================================

    @RestController
    @RequestMapping("/api/day50/accounts")
    @Slf4j
    public static class AccountController {

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public RegisterResponse register(@Valid @RequestBody RegisterRequest req) {
            log.debug("[Day50] Registering user: {}", req.username());
            return new RegisterResponse(req.username(), req.email(), "Account created successfully");
        }
    }
}
