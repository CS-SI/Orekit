/* Contributed in the public domain.
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.events;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventsLogger;
import org.orekit.propagation.events.EventsLogger.LoggedEvent;
import org.orekit.propagation.events.FieldOfView;
import org.orekit.propagation.events.GroundFieldOfViewDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.util.List;

/**
 * Unit tests for {@link GroundFieldOfViewDetector}.
 *
 * @author Evan Ward
 */
public class GroundFieldOfViewDetectorTest {

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * Check FoV detector is similar to {@link ElevationDetector} when using
     * zenith pointing.
     *
     * @throws OrekitException on error.
     */
    @Test
    public void testCaseSimilarToElevationDetector() throws OrekitException {
        //setup
        double pi = FastMath.PI;
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH; //arbitrary date
        AbsoluteDate endDate = date.shiftedBy(Constants.JULIAN_DAY);
        Frame eci = FramesFactory.getGCRF();
        Frame ecef = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        BodyShape earth = new OneAxisEllipsoid(
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                ecef);
        GeodeticPoint gp = new GeodeticPoint(
                FastMath.toRadians(39), FastMath.toRadians(77), 0);
        TopocentricFrame topo = new TopocentricFrame(earth, gp, "topo");
        //iss like orbit
        KeplerianOrbit orbit = new KeplerianOrbit(
                6378137 + 400e3, 0, FastMath.toRadians(51.65), 0, 0, 0,
                PositionAngle.TRUE, eci, date, Constants.EGM96_EARTH_MU);
        Propagator prop = new KeplerianPropagator(orbit);

        //compute expected result
        ElevationDetector elevationDetector =
                new ElevationDetector(topo).withConstantElevation(pi / 6)
                        .withMaxCheck(5.0);
        EventsLogger logger = new EventsLogger();
        prop.addEventDetector(logger.monitorDetector(elevationDetector));
        prop.propagate(endDate);
        List<LoggedEvent> expected = logger.getLoggedEvents();

        //action
        //construct similar FoV based detector
        //half width of 60 deg pointed along +Z in antenna frame
        //not a perfect small circle b/c FoV makes a polygon with great circles
        FieldOfView fov =
                new FieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I, pi / 3, 16, 0);
        //simple case for fixed pointing to be similar to elevation detector.
        //could define new frame with varying rotation for slewing antenna.
        GroundFieldOfViewDetector fovDetector =
                new GroundFieldOfViewDetector(topo, fov)
                        .withMaxCheck(5.0);
        Assert.assertSame(topo, fovDetector.getFrame());
        Assert.assertSame(fov, fovDetector.getFieldOfView());
        logger = new EventsLogger();

        prop = new KeplerianPropagator(orbit);
        prop.addEventDetector(logger.monitorDetector(fovDetector));
        prop.propagate(endDate);
        List<LoggedEvent> actual = logger.getLoggedEvents();

        //verify
        Assert.assertEquals(2, expected.size());
        Assert.assertEquals(2, actual.size());
        for (int i = 0; i < 2; i++) {
            AbsoluteDate expectedDate = expected.get(i).getState().getDate();
            AbsoluteDate actualDate = actual.get(i).getState().getDate();
            // same event times to within 1s.
            Assert.assertEquals(expectedDate.durationFrom(actualDate), 0.0, 1.0);
        }

    }

}
