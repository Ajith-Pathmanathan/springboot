package com.techleadguru.phase4.day70;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAY 70 — Cron Expression Mastery
 *
 * SPRING CRON FORMAT (6 fields — includes seconds):
 *   second  minute  hour  dayOfMonth  month  dayOfWeek
 *   ─────── ─────── ──── ─────────── ─────  ──────────
 *   0-59    0-59    0-23  1-31       1-12   0-7 (0,7=Sunday)
 *                          or ?      or MON-SUN
 *
 * NOTE: Spring uses 6-field cron (with seconds). Unix cron uses 5 fields (no seconds).
 *
 * FIELD OPERATORS:
 *   *   = every value (every second, every minute, etc.)
 *   ?   = no specific value (required for dayOfMonth OR dayOfWeek when other is set)
 *   -   = range: 9-17 = hours 9 through 17
 *   ,   = list: MON,WED,FRI = Monday, Wednesday, Friday
 *   /   = step: 0/15 = start at 0, every 15 units → 0, 15, 30, 45
 *   L   = last: L in dayOfMonth = last day of month; 5L in dayOfWeek = last Friday
 *   W   = nearest weekday: 15W = nearest weekday to the 15th
 *   #   = Nth weekday: 2#3 = third Monday of month
 *
 * COMMON PATTERNS:
 *   "0 0 * * * *"         every hour at :00:00
 *   "0 0/15 * * * *"      every 15 minutes
 *   "0 0 9-17 * * MON-FRI" M-F office hours, top of every hour
 *   "0 0 2 * * *"         daily at 02:00:00
 *   "0 0 2 1 * *"         first day of each month at 02:00:00
 *   "0 0 0 * * 0"         every Sunday at midnight
 *   "0 0 9 ? * MON"       every Monday at 09:00:00
 *   "0 0 0 L * ?"         last day of every month at midnight
 *   "0 0 8 ? * 2#1"       first Monday of every month at 08:00:00
 *
 * TIMEZONE:
 *   @Scheduled(cron = "0 0 2 * * *", zone = "America/New_York")
 *   Without zone: uses JVM default timezone — DANGEROUS if servers in different TZ.
 *   Always set zone explicitly for production cron jobs.
 *
 * VALIDATION:
 *   CronExpression.parse("0 0 2 * * *") → throws IllegalArgumentException if invalid
 *   Use this to validate user-provided cron expressions.
 */
@Slf4j
public class Day70CronMastery {

    // =========================================================================
    // Cron expression validator and next-fire-time calculator
    // =========================================================================

    public static class CronHelper {

        /**
         * Validates a cron expression. Returns null on success or error message.
         */
        public static String validate(String expression) {
            try {
                CronExpression.parse(expression);
                return null; // valid
            } catch (IllegalArgumentException e) {
                return "Invalid cron: " + e.getMessage();
            }
        }

        /**
         * Calculates the next N fire times for a cron expression.
         */
        public static List<ZonedDateTime> nextFireTimes(String expression, int count) {
            return nextFireTimes(expression, count, ZoneId.systemDefault());
        }

        public static List<ZonedDateTime> nextFireTimes(String expression, int count, ZoneId zone) {
            CronExpression cron = CronExpression.parse(expression);
            List<ZonedDateTime> times = new ArrayList<>();
            ZonedDateTime next = ZonedDateTime.now(zone);
            for (int i = 0; i < count; i++) {
                next = cron.next(next);
                if (next == null) break;
                times.add(next);
            }
            return times;
        }

        /**
         * Describes a cron expression in plain English (common patterns).
         */
        public static String describe(String expression) {
            return switch (expression.trim()) {
                case "0 0 * * * *"         -> "Every hour at :00";
                case "0 0/15 * * * *"      -> "Every 15 minutes";
                case "0 0/30 * * * *"      -> "Every 30 minutes";
                case "0 0 2 * * *"         -> "Daily at 02:00";
                case "0 0 0 * * *"         -> "Daily at midnight";
                case "0 0 9 * * MON-FRI"   -> "Weekdays at 09:00";
                case "0 0 0 1 * *"         -> "First day of every month at midnight";
                case "0 0 0 * * 0"         -> "Every Sunday at midnight";
                default -> "Custom cron: " + expression + " (use CronExpression.parse to verify)";
            };
        }
    }

    // =========================================================================
    // Common cron constants for reuse across the codebase
    // =========================================================================

    public static final class CronSchedules {
        public static final String EVERY_MINUTE           = "0 * * * * *";
        public static final String EVERY_15_MINUTES       = "0 0/15 * * * *";
        public static final String EVERY_HOUR             = "0 0 * * * *";
        public static final String DAILY_2AM              = "0 0 2 * * *";
        public static final String DAILY_MIDNIGHT         = "0 0 0 * * *";
        public static final String WEEKDAYS_9AM           = "0 0 9 * * MON-FRI";
        public static final String EVERY_MONDAY_9AM       = "0 0 9 ? * MON";
        public static final String FIRST_OF_MONTH_2AM     = "0 0 2 1 * *";
        public static final String LAST_OF_MONTH_MIDNIGHT = "0 0 0 L * ?";
        public static final String SUNDAYS_MIDNIGHT       = "0 0 0 * * 0";

        private CronSchedules() {}
    }
}
