package com.techleadguru.phase4.day70;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 70 — Cron Expression Mastery Test
 *
 * Verifies:
 * 1. CronHelper.validate() returns null for valid expressions, error for invalid
 * 2. CronHelper.nextFireTimes() returns the correct number of future dates
 * 3. CronSchedules constants are all valid Spring cron expressions
 * 4. CronHelper.describe() returns human-readable descriptions
 *
 * Note: No Spring context needed — CronHelper is a pure utility class.
 */
class Day70CronMasteryTest {

    @Test
    void validate_returns_null_for_valid_cron() {
        assertThat(Day70CronMastery.CronHelper.validate("0 0 2 * * *")).isNull();
        assertThat(Day70CronMastery.CronHelper.validate("0 0/15 * * * *")).isNull();
        assertThat(Day70CronMastery.CronHelper.validate("0 0 9 * * MON-FRI")).isNull();
    }

    @Test
    void validate_returns_error_message_for_invalid_cron() {
        String error = Day70CronMastery.CronHelper.validate("not-a-cron");
        assertThat(error).isNotBlank();
        assertThat(error.toLowerCase()).containsAnyOf("invalid", "error", "parse");
    }

    @Test
    void validate_returns_error_for_wrong_field_count() {
        // Spring cron requires exactly 6 fields (unlike Unix cron's 5)
        String error = Day70CronMastery.CronHelper.validate("0 0 * * *"); // only 5 fields
        assertThat(error).isNotBlank();
    }

    @Test
    void nextFireTimes_returns_requested_count() {
        List<?> times = Day70CronMastery.CronHelper.nextFireTimes("0 0 9 * * MON-FRI", 5);
        assertThat(times).hasSize(5);
    }

    @Test
    void nextFireTimes_all_in_future() {
        List<ZonedDateTime> times = Day70CronMastery.CronHelper.nextFireTimes("0 0 2 * * *", 3);
        assertThat(times).hasSize(3);
        times.forEach(t -> assertThat(t).isAfter(ZonedDateTime.now().minusMinutes(1)));
    }

    @Test
    void nextFireTimes_with_timezone() {
        var timesUtc = Day70CronMastery.CronHelper.nextFireTimes(
                "0 0 9 * * MON", 3, ZoneId.of("UTC"));
        var timesNy = Day70CronMastery.CronHelper.nextFireTimes(
                "0 0 9 * * MON", 3, ZoneId.of("America/New_York"));
        assertThat(timesUtc).hasSize(3);
        assertThat(timesNy).hasSize(3);
        // NY is UTC-5/4, so the fire times are different
        assertThat(timesUtc).isNotEqualTo(timesNy);
    }

    @Test
    void cron_schedules_constants_are_all_valid() {
        assertThat(Day70CronMastery.CronHelper.validate(Day70CronMastery.CronSchedules.EVERY_MINUTE)).isNull();
        assertThat(Day70CronMastery.CronHelper.validate(Day70CronMastery.CronSchedules.EVERY_HOUR)).isNull();
        assertThat(Day70CronMastery.CronHelper.validate(Day70CronMastery.CronSchedules.DAILY_2AM)).isNull();
        assertThat(Day70CronMastery.CronHelper.validate(Day70CronMastery.CronSchedules.WEEKDAYS_9AM)).isNull();
        assertThat(Day70CronMastery.CronHelper.validate(Day70CronMastery.CronSchedules.FIRST_OF_MONTH_2AM)).isNull();
    }

    @Test
    void describe_returns_human_readable_text() {
        String description = Day70CronMastery.CronHelper.describe("0 0 2 * * *");
        assertThat(description).isNotBlank();
    }

    @Test
    void every_minute_fires_60_times_per_hour() {
        List<ZonedDateTime> times = Day70CronMastery.CronHelper.nextFireTimes(Day70CronMastery.CronSchedules.EVERY_MINUTE, 60);
        assertThat(times).hasSize(60);
        // Verify all are within 1 hour of each other
        ZonedDateTime first = times.get(0);
        ZonedDateTime last  = times.get(59);
        assertThat(java.time.Duration.between(first, last).toMinutes()).isEqualTo(59);
    }
}
