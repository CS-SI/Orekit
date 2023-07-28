package org.orekit.time;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimeStampedDoubleHermiteInterpolatorTest {

    @Test
    @DisplayName("Test default constructor")
    void testDefaultConstructor() {
        // When
        final TimeStampedDoubleHermiteInterpolator interpolator = new TimeStampedDoubleHermiteInterpolator();

        // Then
        Assertions.assertEquals(AbstractTimeInterpolator.DEFAULT_INTERPOLATION_POINTS,
                                interpolator.getNbInterpolationPoints());
        Assertions.assertEquals(AbstractTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC,
                                interpolator.getExtrapolationThreshold());
    }
}