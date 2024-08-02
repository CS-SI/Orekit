package org.orekit.propagation.conversion.averaging.converters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.conversion.averaging.EcksteinHechlerOrbitalState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OsculatingToEcksteinHechlerElementsConverterTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data:potential");
    }

    @Test
    void testConvertToAveragedElements() {
        // GIVEN
        final CircularOrbit osculatingOrbit = new CircularOrbit(1e7, 2e-4, -1e-4, 1.e-3, 2., -3.,
                PositionAngleType.MEAN, FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH,
                Constants.EGM96_EARTH_MU);
        final OsculatingToEcksteinHechlerConverter converter = new OsculatingToEcksteinHechlerConverter(getProvider());
        converter.setEpsilon(1e-12);
        converter.setMaxIterations(100);
        // WHEN
        final EcksteinHechlerOrbitalState averagedElements = converter
                .convertToAveraged(osculatingOrbit);
        // THEN
        final Orbit recomputedOsculatingOrbit = averagedElements.toOsculatingOrbit();
        final PVCoordinates relativePV = new PVCoordinates(osculatingOrbit.getPVCoordinates(),
                recomputedOsculatingOrbit.getPVCoordinates(osculatingOrbit.getFrame()));
        final double expectedDifference = 0.;
        assertEquals(expectedDifference, relativePV.getPosition().getNorm(), 1e-7);
        assertEquals(expectedDifference, relativePV.getVelocity().getNorm(), 3e-2);
    }

    private UnnormalizedSphericalHarmonicsProvider getProvider() {
        return DataContext.getDefault().getGravityFields().getUnnormalizedProvider(6, 0);
    }

}
