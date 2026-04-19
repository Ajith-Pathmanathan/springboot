package com.techleadguru.phase5.day93;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class Day93GcAlgorithmsTest {

    @Test
    void g1gcFlags_contains_UseG1GC() {
        assertThat(Day93GcAlgorithms.GcConfig.g1gcFlags()).contains("-XX:+UseG1GC");
    }

    @Test
    void zgcFlags_contains_UseZGC() {
        assertThat(Day93GcAlgorithms.GcConfig.zgcFlags()).contains("-XX:+UseZGC");
    }

    @Test
    void shenandoahFlags_contains_UseShenandoahGC() {
        assertThat(Day93GcAlgorithms.GcConfig.shenandoahFlags()).contains("-XX:+UseShenandoahGC");
    }

    @Test
    void detectCurrentGc_returns_non_null() {
        String gc = Day93GcAlgorithms.GcConfig.detectCurrentGc();
        assertThat(gc).isNotNull().isNotBlank();
    }

    @Test
    void selectGcForWorkload_recommends_zgc_for_sub_ms_pause() {
        var profile = new Day93GcAlgorithms.WorkloadProfile(1, 8192, true, true);
        assertThat(Day93GcAlgorithms.selectGcForWorkload(profile))
                .isEqualTo(Day93GcAlgorithms.GcRecommendation.ZGC);
    }

    @Test
    void selectGcForWorkload_recommends_shenandoah_for_short_pause_openJdk() {
        var profile = new Day93GcAlgorithms.WorkloadProfile(20, 8192, false, true);
        assertThat(Day93GcAlgorithms.selectGcForWorkload(profile))
                .isEqualTo(Day93GcAlgorithms.GcRecommendation.SHENANDOAH);
    }

    @Test
    void selectGcForWorkload_recommends_parallel_for_throughput_small_heap() {
        var profile = new Day93GcAlgorithms.WorkloadProfile(200, 2048, false, false);
        assertThat(Day93GcAlgorithms.selectGcForWorkload(profile))
                .isEqualTo(Day93GcAlgorithms.GcRecommendation.PARALLEL);
    }

    @Test
    void selectGcForWorkload_recommends_g1gc_as_default() {
        var profile = new Day93GcAlgorithms.WorkloadProfile(200, 8192, false, false);
        assertThat(Day93GcAlgorithms.selectGcForWorkload(profile))
                .isEqualTo(Day93GcAlgorithms.GcRecommendation.G1GC);
    }

    @Test
    void g1RegionSimulator_simulateLayout_correct_region_count() {
        var regions = Day93GcAlgorithms.G1RegionSimulator.simulateLayout(100);
        assertThat(regions).hasSize(100);
    }

    @Test
    void g1RegionSimulator_layout_has_mixed_types() {
        var regions = Day93GcAlgorithms.G1RegionSimulator.simulateLayout(100);
        var types = regions.stream()
                .map(Day93GcAlgorithms.G1RegionSimulator.Region::type)
                .distinct()
                .toList();
        assertThat(types).hasSizeGreaterThan(1);
    }

    @Test
    void buildCollectionSet_respects_max_regions_limit() {
        var regions = Day93GcAlgorithms.G1RegionSimulator.simulateLayout(100);
        var cset = Day93GcAlgorithms.G1RegionSimulator.buildCollectionSet(regions, 5);
        assertThat(cset).hasSizeLessThanOrEqualTo(5);
    }
}
