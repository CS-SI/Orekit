package org.orekit.propagation.conversion;

import org.hipparchus.ode.AbstractIntegrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

class AbstractVariableStepIntegratorBuilderTest {

    @Test
    void testGetTolerances() {
        // GIVEN
        final Orbit orbit = new KeplerianOrbit(7e6, 0.1, 1, 2, 3, 4, PositionAngleType.ECCENTRIC, FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final double dP = 1.;
        final double dV = 0.001;
        final TestIntegratorBuilder integratorBuilder = new TestIntegratorBuilder(0., 1., dP, dV);
        // WHEN
        final double[][] actualTolerances = integratorBuilder.getTolerances(orbit, orbit.getType());
        // THEN
        final double [][] expectedTolerances = NumericalPropagator.tolerances(dP, dV, orbit, orbit.getType());
        Assertions.assertArrayEquals(expectedTolerances[0], actualTolerances[0]);
        Assertions.assertArrayEquals(expectedTolerances[1], actualTolerances[1]);
    }

    private static class TestIntegratorBuilder extends AbstractVariableStepIntegratorBuilder {

        protected TestIntegratorBuilder(double minStep, double maxStep, double dP, double dV) {
            super(minStep, maxStep, dP, dV);
        }

        @Override
        public AbstractIntegrator buildIntegrator(Orbit orbit, OrbitType orbitType) {
            return null;
        }
    }
}
