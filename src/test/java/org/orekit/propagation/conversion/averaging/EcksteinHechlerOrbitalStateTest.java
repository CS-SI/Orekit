package org.orekit.propagation.conversion.averaging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.conversion.averaging.elements.AveragedCircularWithMeanAngle;
import org.orekit.time.AbsoluteDate;

class EcksteinHechlerOrbitalStateTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:potential");
    }
    
    @Test
    void testToOsculating() {
        // GIVEN
        final UnnormalizedSphericalHarmonicsProvider provider = getProvider();
        final EcksteinHechlerOrbitalState averagedState = new EcksteinHechlerOrbitalState(AbsoluteDate.ARBITRARY_EPOCH,
                new AveragedCircularWithMeanAngle(1e7, -0.01, 0., 0.01, 0.,
                0.), FramesFactory.getGCRF(), provider);
        // WHEN
        final Orbit orbit = averagedState.toOsculatingOrbit();
        final CircularOrbit circularOrbit = (CircularOrbit) averagedState.getOrbitType().convertType(orbit);
        // THEN
        Assertions.assertEquals(averagedState.getDate(), circularOrbit.getDate());
        compareOrbitalElements(averagedState.getAveragedElements(), circularOrbit,
                averagedState.getPositionAngleType());
    }

    private void compareOrbitalElements(final AveragedCircularWithMeanAngle elements,
                                        final CircularOrbit circularOrbit,
                                        final PositionAngleType positionAngleType) {
        Assertions.assertEquals(elements.getAveragedSemiMajorAxis(), circularOrbit.getA(), 1e4);
        Assertions.assertEquals(elements.getAveragedCircularEx(), circularOrbit.getCircularEx(), 1e-3);
        Assertions.assertEquals(elements.getAveragedCircularEy(), circularOrbit.getCircularEy(), 1e-3);
        Assertions.assertEquals(elements.getAveragedInclination(), circularOrbit.getI(), 1e-3);
        Assertions.assertEquals(elements.getAveragedRightAscensionOfTheAscendingNode(),
                circularOrbit.getRightAscensionOfAscendingNode(), 1e-3);
        Assertions.assertEquals(elements.getAveragedMeanLatitudeArgument(),
                circularOrbit.getAlpha(positionAngleType), 1.1e-3);
    }

    private UnnormalizedSphericalHarmonicsProvider getProvider() {
        return DataContext.getDefault().getGravityFields().getUnnormalizedProvider(6, 0);
    }

}
