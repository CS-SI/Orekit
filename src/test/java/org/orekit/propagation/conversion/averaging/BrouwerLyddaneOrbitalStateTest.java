package org.orekit.propagation.conversion.averaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.conversion.averaging.elements.AveragedKeplerianWithMeanAngle;
import org.orekit.time.AbsoluteDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrouwerLyddaneOrbitalStateTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data:potential");
    }
    
    @Test
    void testToOsculating() {
        // GIVEN
        final UnnormalizedSphericalHarmonicsProvider provider = getProvider(5);
        final BrouwerLyddaneOrbitalState averagedState = new BrouwerLyddaneOrbitalState(AbsoluteDate.ARBITRARY_EPOCH,
                new AveragedKeplerianWithMeanAngle(1e7, 0.1, 1., 2., 3.,
                -1.), FramesFactory.getGCRF(), provider);
        // WHEN
        final Orbit orbit = averagedState.toOsculatingOrbit();
        final KeplerianOrbit keplerianOrbit = (KeplerianOrbit) averagedState.getOrbitType()
                .convertType(orbit);
        // THEN
        assertEquals(averagedState.getDate(), keplerianOrbit.getDate());
        compareOrbitalElements(averagedState.getAveragedElements(), keplerianOrbit,
                averagedState.getPositionAngleType());
    }

    private void compareOrbitalElements(final AveragedKeplerianWithMeanAngle elements,
                                        final KeplerianOrbit keplerianOrbit,
                                        final PositionAngleType positionAngleType) {
        assertEquals(elements.getAveragedSemiMajorAxis(), keplerianOrbit.getA(), 1e4);
        assertEquals(elements.getAveragedEccentricity(), keplerianOrbit.getE(), 1e-3);
        assertEquals(elements.getAveragedInclination(), keplerianOrbit.getI(), 1e-3);
        assertEquals(elements.getAveragedPerigeeArgument(),
                keplerianOrbit.getPerigeeArgument(), 1e-2);
        assertEquals(elements.getAveragedRightAscensionOfTheAscendingNode(),
                keplerianOrbit.getRightAscensionOfAscendingNode(), 1e-3);
        assertEquals(elements.getAveragedMeanAnomaly(),
                keplerianOrbit.getAnomaly(positionAngleType), 1e-2);
    }

    private UnnormalizedSphericalHarmonicsProvider getProvider(final int maxDegree) {
        return DataContext.getDefault().getGravityFields().getUnnormalizedProvider(maxDegree, 0);
    }

}
