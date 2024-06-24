package org.orekit.propagation.conversion.averaging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.conversion.averaging.elements.AveragedKeplerianWithMeanAngle;
import org.orekit.time.AbsoluteDate;

class SGP4OrbitalStateTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testToOsculating() {
        final SGP4OrbitalState averagedState = new SGP4OrbitalState(AbsoluteDate.ARBITRARY_EPOCH,
                new AveragedKeplerianWithMeanAngle(1e7, 0.1, 1., 2., 3., -1.));
        // WHEN
        final Orbit orbit = averagedState.toOsculatingOrbit();
        final KeplerianOrbit keplerianOrbit = (KeplerianOrbit) averagedState.getOrbitType().convertType(orbit);
        // THEN
        Assertions.assertEquals(averagedState.getDate(), keplerianOrbit.getDate());
        Assertions.assertEquals(averagedState.getFrame(), keplerianOrbit.getFrame());
        compareOrbitalElements(averagedState.getAveragedElements(), keplerianOrbit,
                averagedState.getPositionAngleType());
    }

    private void compareOrbitalElements(final AveragedKeplerianWithMeanAngle elements,
                                        final KeplerianOrbit keplerianOrbit,
                                        final PositionAngleType positionAngleType) {
        Assertions.assertEquals(elements.getAveragedSemiMajorAxis(), keplerianOrbit.getA(), 1e3);
        Assertions.assertEquals(elements.getAveragedEccentricity(), keplerianOrbit.getE(), 1e-3);
        Assertions.assertEquals(elements.getAveragedInclination(), keplerianOrbit.getI(), 1e-3);
        Assertions.assertEquals(elements.getAveragedPerigeeArgument(),
                keplerianOrbit.getPerigeeArgument(), 1e-2);
        Assertions.assertEquals(elements.getAveragedRightAscensionOfTheAscendingNode(),
                keplerianOrbit.getRightAscensionOfAscendingNode(), 1e-3);
        Assertions.assertEquals(elements.getAveragedMeanAnomaly(),
                keplerianOrbit.getAnomaly(positionAngleType), 1e-2);
    }

    @Test
    void testOf() {
        // GIVEN
        final String line1SPOT = "1 22823U 93061A   03339.49496229  .00000173  00000-0  10336-3 0   133";
        final String line2SPOT = "2 22823  98.4132 359.2998 0017888 100.4310 259.8872 14.18403464527664";
        final TLE tle = new TLE(line1SPOT, line2SPOT);
        final Frame teme = FramesFactory.getTEME();
        // WHEN
        final SGP4OrbitalState orbitalState = SGP4OrbitalState.of(tle, teme);
        // THEN
        final AveragedKeplerianWithMeanAngle elements = orbitalState.getAveragedElements();
        Assertions.assertEquals(tle.getE(), elements.getAveragedEccentricity());
        Assertions.assertEquals(tle.getI(), elements.getAveragedInclination());
        Assertions.assertEquals(tle.getRaan(), elements.getAveragedRightAscensionOfTheAscendingNode());
        Assertions.assertEquals(tle.getPerigeeArgument(), elements.getAveragedPerigeeArgument());
        Assertions.assertEquals(tle.getMeanAnomaly(), elements.getAveragedMeanAnomaly());
    }

}
