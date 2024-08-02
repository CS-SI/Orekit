package org.orekit.propagation.conversion.averaging.converters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.conversion.averaging.SGP4OrbitalState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OsculatingToSGP4ElementsConverterTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data:potential");
    }

    @Test
    void testConvertToAveragedElements() {
        // GIVEN
        final KeplerianOrbit osculatingOrbit = new KeplerianOrbit(1e7, 0.1, 1., 1., 2., -3.,
                PositionAngleType.MEAN, FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH,
                Constants.EGM96_EARTH_MU);
        final OsculatingToSGP4Converter converter = new OsculatingToSGP4Converter();
        converter.setEpsilon(1e-12);
        converter.setMaxIterations(100);
        // WHEN
        final SGP4OrbitalState averagedElements = converter
                .convertToAveraged(osculatingOrbit);
        // THEN
        final Orbit recomputedOsculatingOrbit = averagedElements.toOsculatingOrbit();
        final PVCoordinates relativePV = new PVCoordinates(osculatingOrbit.getPVCoordinates(),
                recomputedOsculatingOrbit.getPVCoordinates(osculatingOrbit.getFrame()));
        final double expectedDifference = 0.;
        assertEquals(expectedDifference, relativePV.getPosition().getNorm(), 1e-5);
        assertEquals(expectedDifference, relativePV.getVelocity().getNorm(), 3e-3);
    }

}
