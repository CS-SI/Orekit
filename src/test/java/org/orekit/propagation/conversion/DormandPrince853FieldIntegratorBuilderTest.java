package org.orekit.propagation.conversion;

import org.hipparchus.complex.Complex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

class DormandPrince853FieldIntegratorBuilderTest {

    @Test
    void testGetTolerances() {
        // GIVEN
        final double minStep = 1;
        final double maxStep = 10;
        final double dP = 1;
        final double dV = Double.NaN;
        final Orbit orbit = new KeplerianOrbit(7e6, 0.1, 1, 2, 3, 4, PositionAngleType.ECCENTRIC, FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        // WHEN
        final DormandPrince853FieldIntegratorBuilder<Complex> builder = new DormandPrince853FieldIntegratorBuilder<>(minStep,
                maxStep, dP, dV);
        final double[][] actualTolerances = builder.getTolerances(orbit, orbit.getType());
        // THEN
        final DormandPrince853FieldIntegratorBuilder<Complex> builder2 = new DormandPrince853FieldIntegratorBuilder<>(minStep,
                maxStep, dP);
        final double[][] expectedTolerances = builder2.getTolerances(orbit, orbit.getType());
        Assertions.assertArrayEquals(expectedTolerances[0], actualTolerances[0]);
        Assertions.assertArrayEquals(expectedTolerances[1], actualTolerances[1]);
    }

}
