package org.orekit.propagation.conversion;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince54Integrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;

import java.util.Arrays;

class AbstractVariableStepIntegratorBuilderTest {

    @Test
    void testGetTolerancesOrbit() {
        // GIVEN
        final Orbit orbit = new KeplerianOrbit(7e6, 0.1, 1, 2, 3, 4, PositionAngleType.ECCENTRIC, FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final double expectedAbsoluteTolerance = 1.;
        final double expectedRelativeTolerance = 0.001;
        final TestIntegratorBuilder integratorBuilder = new TestIntegratorBuilder(0., 1.,
                ToleranceProvider.of(expectedAbsoluteTolerance, expectedRelativeTolerance));
        // WHEN
        final double[][] actualTolerances = integratorBuilder.getTolerances(orbit, orbit.getType());
        // THEN
        final double[] expectedAbsolute = new double[7];
        Arrays.fill(expectedAbsolute, expectedAbsoluteTolerance);
        final double[] expectedRelative = new double[7];
        Arrays.fill(expectedRelative, expectedRelativeTolerance);
        final double [][] expectedTolerances = new double[][] {expectedAbsolute, expectedRelative};
        Assertions.assertArrayEquals(expectedTolerances[0], actualTolerances[0]);
        Assertions.assertArrayEquals(expectedTolerances[1], actualTolerances[1]);
    }

    @Test
    void testGetTolerances() {
        // GIVEN
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, Vector3D.MINUS_J, Vector3D.MINUS_K);
        final double expectedAbsoluteTolerance = 1.;
        final double expectedRelativeTolerance = 0.001;
        final TestIntegratorBuilder integratorBuilder = new TestIntegratorBuilder(0., 1.,
                ToleranceProvider.of(expectedAbsoluteTolerance, expectedRelativeTolerance));
        // WHEN
        final double[][] actualTolerances = integratorBuilder.getTolerances(absolutePVCoordinates);
        // THEN
        final double[] expectedAbsolute = new double[7];
        Arrays.fill(expectedAbsolute, expectedAbsoluteTolerance);
        final double[] expectedRelative = new double[7];
        Arrays.fill(expectedRelative, expectedRelativeTolerance);
        final double [][] expectedTolerances = new double[][] {expectedAbsolute, expectedRelative};
        Assertions.assertArrayEquals(expectedTolerances[0], actualTolerances[0]);
        Assertions.assertArrayEquals(expectedTolerances[1], actualTolerances[1]);
    }

    private static class TestIntegratorBuilder extends AbstractVariableStepIntegratorBuilder<DormandPrince54Integrator> {

        protected TestIntegratorBuilder(double minStep, double maxStep, ToleranceProvider toleranceProvider) {
            super(minStep, maxStep, toleranceProvider);
        }

        @Override
        protected DormandPrince54Integrator buildIntegrator(double[][] tolerances) {
            return null;
        }
    }
}
