package org.orekit.forces.maneuvers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.forces.maneuvers.propulsion.BasicConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.propulsion.PropulsionModel;
import org.orekit.forces.maneuvers.trigger.DateBasedManeuverTriggers;
import org.orekit.time.AbsoluteDate;

class ManeuverTest {

    @Test
    void testGetName() {
        // GIVEN
        final String tooShortName = "a";
        final String expectedName = "aa";
        final double arbitraryDuration = 0.;
        final DateBasedManeuverTriggers triggers = new DateBasedManeuverTriggers(tooShortName, AbsoluteDate.ARBITRARY_EPOCH, arbitraryDuration);
        final double arbitraryThrust = 1.;
        final double arbitraryIsp = 1.;
        final PropulsionModel propulsion = new BasicConstantThrustPropulsionModel(arbitraryThrust, arbitraryIsp, Vector3D.PLUS_I, expectedName);
        // WHEN
        final Maneuver maneuver = new Maneuver(null, triggers, propulsion);
        final String actualName = maneuver.getName();
        // THEN
        Assertions.assertEquals(expectedName, actualName);
    }

}