package dev.bykowski.ssisw.simulation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoPacingTests {

    @Test
    void normalModeHasNoDelays() {
        assertThat(DemoPacing.forMode(false, 15_000, 1_000)).isEqualTo(new DemoPacing(0, 0));
    }

    @Test
    void demoModeUsesConfiguredDelays() {
        assertThat(DemoPacing.forMode(true, 15_000, 1_000)).isEqualTo(new DemoPacing(15_000, 1_000));
    }

    @Test
    void rejectsNegativeDelaysWhenEnabled() {
        assertThatThrownBy(() -> DemoPacing.forMode(true, -1, 1_000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DemoPacing.forMode(true, 15_000, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
