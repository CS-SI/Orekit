package org.orekit.propagation.conversion;

import org.hipparchus.Field;
import org.hipparchus.ode.nonstiff.DormandPrince54FieldIntegrator;
import org.hipparchus.util.Binary64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

import java.util.Arrays;

class AbstractVariableStepFieldIntegratorBuilderTest {

    @Test
    void testGetTolerances() {
        // GIVEN
        final Orbit orbit = new KeplerianOrbit(7e6, 0.1, 1, 2, 3, 4, PositionAngleType.ECCENTRIC, FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final double expectedAbsoluteTolerance = 1.;
        final double expectedRelativeTolerance = 2;
        final TestIntegratorBuilder integratorBuilder = new TestIntegratorBuilder(0., 1.,
                ToleranceProvider.of(expectedAbsoluteTolerance, expectedRelativeTolerance));
        // WHEN
        final double[][] actualTolerances = integratorBuilder.getTolerances(orbit, orbit.getType(), PositionAngleType.TRUE);
        // THEN
        final double[] expectedAbsolute = new double[7];
        Arrays.fill(expectedAbsolute, expectedAbsoluteTolerance);
        final double[] expectedRelative = new double[7];
        Arrays.fill(expectedRelative, expectedRelativeTolerance);
        final double [][] expectedTolerances = new double[][] {expectedAbsolute, expectedRelative};
        Assertions.assertArrayEquals(expectedTolerances[0], actualTolerances[0]);
        Assertions.assertArrayEquals(expectedTolerances[1], actualTolerances[1]);
    }

    private static class TestIntegratorBuilder extends AbstractVariableStepFieldIntegratorBuilder<Binary64, DormandPrince54FieldIntegrator<Binary64>> {

        protected TestIntegratorBuilder(double minStep, double maxStep, ToleranceProvider toleranceProvider) {
            super(minStep, maxStep, toleranceProvider);
        }

        @Override
        protected DormandPrince54FieldIntegrator<Binary64> buildIntegrator(Field field, double[][] tolerances) {
            return null;
        }

        @Override
        public ODEIntegratorBuilder toODEIntegratorBuilder() {
            return null;
        }
    }
}
