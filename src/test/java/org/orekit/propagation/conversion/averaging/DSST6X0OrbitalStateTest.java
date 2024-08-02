package org.orekit.propagation.conversion.averaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.conversion.averaging.elements.AveragedEquinoctialWithMeanAngle;
import org.orekit.time.AbsoluteDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DSST6X0OrbitalStateTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data:potential");
    }
    
    @Test
    void testToOsculating() {
        // GIVEN
        final UnnormalizedSphericalHarmonicsProvider provider = getProvider();
        final DSST6X0OrbitalState averagedState = new DSST6X0OrbitalState(AbsoluteDate.ARBITRARY_EPOCH,
                new AveragedEquinoctialWithMeanAngle(1e7, 0.1, 0., 0.2, -0.3,
                -1.), FramesFactory.getGCRF(), provider);
        // WHEN
        final Orbit orbit = averagedState.toOsculatingOrbit();
        final EquinoctialOrbit equinoctialOrbit = (EquinoctialOrbit) averagedState.getOrbitType().convertType(orbit);
        // THEN
        assertEquals(averagedState.getDate(), equinoctialOrbit.getDate());
        compareOrbitalElements(averagedState.getAveragedElements(), equinoctialOrbit,
                averagedState.getPositionAngleType());
    }

    private void compareOrbitalElements(final AveragedEquinoctialWithMeanAngle elements,
                                        final EquinoctialOrbit equinoctialOrbit,
                                        final PositionAngleType positionAngleType) {
        assertEquals(elements.getAveragedSemiMajorAxis(), equinoctialOrbit.getA(), 1.6e4);
        assertEquals(elements.getAveragedEquinoctialEx(),
                equinoctialOrbit.getEquinoctialEx(), 1.e-3);
        assertEquals(elements.getAveragedEquinoctialEy(),
                equinoctialOrbit.getEquinoctialEy(), 2.5e-3);
        assertEquals(elements.getAveragedHx(),
                equinoctialOrbit.getHx(), 4.e-3);
        assertEquals(elements.getAveragedHy(),
                equinoctialOrbit.getHy(), 1.e-3);
        assertEquals(elements.getAveragedMeanLongitudeArgument(),
                equinoctialOrbit.getL(positionAngleType), 1.e-3);
    }

    private UnnormalizedSphericalHarmonicsProvider getProvider() {
        return DataContext.getDefault().getGravityFields().getUnnormalizedProvider(6, 0);
    }

}
