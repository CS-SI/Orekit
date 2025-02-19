package org.orekit.propagation.conversion.averaging.converters;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.conversion.averaging.BrouwerLyddaneOrbitalState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

class OsculatingToBrouwerLyddaneElementsConverterTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:potential");
    }

    @Test
    void testConvertToAveragedElements() {
        // GIVEN
        final KeplerianOrbit osculatingOrbit = new KeplerianOrbit(1e7, 0.1, 1., 1., 2., -3.,
                PositionAngleType.MEAN, FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH,
                Constants.EGM96_EARTH_MU);
        final OsculatingToBrouwerLyddaneConverter converter = new OsculatingToBrouwerLyddaneConverter(getProvider());
        converter.setEpsilon(1e-12);
        converter.setMaxIterations(100);
        // WHEN
        final BrouwerLyddaneOrbitalState averagedElements = converter
                .convertToAveraged(osculatingOrbit);
        // THEN
        final Orbit recomputedOsculatingOrbit = averagedElements.toOsculatingOrbit();
        final PVCoordinates relativePV = new PVCoordinates(osculatingOrbit.getPVCoordinates(),
                recomputedOsculatingOrbit.getPVCoordinates(osculatingOrbit.getFrame()));
        final double expectedDifference = 0.;
        Assertions.assertEquals(expectedDifference, relativePV.getPosition().getNorm(), 1e-5);
        Assertions.assertEquals(expectedDifference, relativePV.getVelocity().getNorm(), 3e-9);
    }

    private UnnormalizedSphericalHarmonicsProvider getProvider() {
        return DataContext.getDefault().getGravityFields().getUnnormalizedProvider(5, 0);
    }

}
