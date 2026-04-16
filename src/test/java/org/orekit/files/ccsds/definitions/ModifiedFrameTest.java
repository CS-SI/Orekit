package org.orekit.files.ccsds.definitions;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Unit tests for {@link ModifiedFrame}.
 *
 * @author Evan Ward
 */
public class ModifiedFrameTest {

    /**
     * Check center and orientation. Which ever part of the translation that is
     * done first (translation or orientation) has zero ULPs of error and the
     * part applied second has a few ULPs. Part of the issue when the rotation
     * is applied second appears to be that the Rotation is not quite a unit
     * quaternion by a few ulps, but distance(IDENTITY) still returns zero
     * because that method ignores q0 when it is not near zero.
     */
    @Test
    public void testFrameTransformations() {
        // setup
        final String centerName = "SSB";
        DataContext context = Utils.newDataContext("regular-data");
        Frame itrf = context.getFrames()
                .getITRF(IERSConventions.IERS_2010, true);
        final CelestialBodyFrame cbf = CelestialBodyFrame.ITRF;
        final CelestialBody ssb =
                context.getCelestialBodies().getSolarSystemBarycenter();
        final CelestialBody earth = context.getCelestialBodies().getEarth();
        // date that has valid auxiliary data
        AbsoluteDate date = context.getTimeScales().getJ2000Epoch()
                .shiftedBy(3 * 30 * Constants.JULIAN_DAY);

        // action
        ModifiedFrame actual = new ModifiedFrame(itrf, cbf, ssb, centerName);

        // verify
        MatcherAssert.assertThat(actual.getRefFrame(), Matchers.is(cbf));
        MatcherAssert.assertThat(actual.getCenterName(), Matchers.is(centerName));
        MatcherAssert.assertThat(actual.getName(),
                Matchers.is("solar system barycenter/CIO/2010-based ITRF simple EOP"));
        MatcherAssert.assertThat(actual.isPseudoInertial(), Matchers.is(false));

        // translating pv
        PVCoordinates earthActual = earth.getPVCoordinates(date, actual);
        PVCoordinates ssbItrf = ssb.getPVCoordinates(date, itrf);
        final PVCoordinates shouldBeZero =
                new PVCoordinates(1, earthActual, 1, ssbItrf);
        // orient first, then translate results in ~ 5e-4 m of position error
        MatcherAssert.assertThat(shouldBeZero,
                OrekitMatchers.pvCloseTo(PVCoordinates.ZERO, 0.0));


        // same orientation as ITRF
        final Transform toItrf = actual.getTransformTo(itrf, date);
        // Rotation.distance ignores q0 which is a few ULPs away from 1.0
        MatcherAssert.assertThat(toItrf.getRotation(),
                OrekitMatchers.distanceIs(Rotation.IDENTITY, Matchers.closeTo(0.0, 0.0)));
        MatcherAssert.assertThat(toItrf.getRotationRate(),
                OrekitMatchers.vectorCloseTo(Vector3D.ZERO, 1e-18));
        MatcherAssert.assertThat(toItrf.getRotationAcceleration(),
                OrekitMatchers.vectorCloseTo(Vector3D.ZERO, 1e-28));

        // same center as SSB
        final Transform pv =
                actual.getTransformTo(ssb.getIcrfAlignedFrame(), date);
        double tol = 0.0;
                //15 * Math.ulp(Constants.JPL_SSD_ASTRONOMICAL_UNIT);
        MatcherAssert.assertThat(pv.getTranslation(),
                OrekitMatchers.vectorCloseTo(Vector3D.ZERO, tol));
        MatcherAssert.assertThat(pv.getVelocity(),
                OrekitMatchers.vectorCloseTo(Vector3D.ZERO, tol));
        MatcherAssert.assertThat(pv.getAcceleration(),
                OrekitMatchers.vectorCloseTo(Vector3D.ZERO, tol));

    }

}
