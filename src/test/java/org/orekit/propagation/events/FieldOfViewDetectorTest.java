/* Copyright 2002-2016 CS Systèmes d'Information
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
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.attitudes.NadirPointing;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.EventsLogger.LoggedEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

import java.util.List;

public class FieldOfViewDetectorTest {

    // Body mu
    private double mu;

    // Computation date
    private AbsoluteDate initDate;

    // Orbit
    private Orbit initialOrbit;

    // WGS84 Earth model
    private OneAxisEllipsoid earth;

    // Earth center pointing attitude provider
    private BodyCenterPointing earthCenterAttitudeLaw;

    @Test
    public void testDihedralFielOfView() throws OrekitException {

        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------

        // Extrapolator definition
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, earthCenterAttitudeLaw);

        // Event definition : square field of view, along X axis, aperture 56°
        final double maxCheck  = 1.;
        final PVCoordinatesProvider sunPV = CelestialBodyFactory.getSun();
        final Vector3D center = Vector3D.MINUS_J;
        final Vector3D axis1 = Vector3D.PLUS_K;
        final Vector3D axis2 = Vector3D.PLUS_I;
        final double aperture1 = FastMath.toRadians(28);
        final double aperture2 = FastMath.toRadians(28);

        final EventDetector sunVisi =
                new FieldOfViewDetector(sunPV, new FieldOfView(center, axis1, aperture1, axis2, aperture2, 0.0)).
                withMaxCheck(maxCheck).
                withHandler(new DihedralSunVisiHandler());

        Assert.assertSame(sunPV, ((FieldOfViewDetector) sunVisi).getPVTarget());
        Assert.assertEquals(0, ((FieldOfViewDetector) sunVisi).getFieldOfView().getMargin(), 1.0e-15);
        double eta = FastMath.acos(FastMath.sin(aperture1) * FastMath.sin(aperture2));
        double theoreticalArea = MathUtils.TWO_PI - 4 * eta;
        Assert.assertEquals(theoreticalArea,
                            ((FieldOfViewDetector) sunVisi).getFieldOfView().getZone().getSize(),
                            1.0e-15);

        // Add event to be detected
        propagator.addEventDetector(sunVisi);

        // Extrapolate from the initial to the final date
        propagator.propagate(initDate.shiftedBy(6000.));

    }

    /** check the default behavior to stop propagation on FoV exit. */
    @Test
    public void testStopOnExit() throws OrekitException {
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
        AttitudeProvider attitude = new NadirPointing(eci, earth);

        //action
        FieldOfView fov =
                new FieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I, pi / 3, 16, 0);
        FieldOfViewDetector fovDetector =
                new FieldOfViewDetector(topo, fov)
                        .withMaxCheck(5.0);
        EventsLogger logger = new EventsLogger();

        Propagator prop = new KeplerianPropagator(orbit, attitude);
        prop.addEventDetector(logger.monitorDetector(fovDetector));
        prop.propagate(endDate);
        List<LoggedEvent> actual = logger.getLoggedEvents();

        //verify
        // check we have an entry and an exit event.
        Assert.assertEquals(2, actual.size());
    }

    @Before
    public void setUp() {
        try {

            Utils.setDataRoot("regular-data");

            // Computation date
            // Satellite position as circular parameters
            mu = 3.9860047e14;

            initDate = new AbsoluteDate(new DateComponents(1969, 8, 28),
                                                     TimeComponents.H00,
                                                     TimeScalesFactory.getUTC());

            Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
            Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
            initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                FramesFactory.getEME2000(), initDate, mu);


            // WGS84 Earth model
            earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                         Constants.WGS84_EARTH_FLATTENING,
                                         FramesFactory.getITRF(IERSConventions.IERS_2010, true));

            // Create earth center pointing attitude provider */
            earthCenterAttitudeLaw = new BodyCenterPointing(initialOrbit.getFrame(), earth);

        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }

    }


    /** Handler for visibility event. */
    private static class DihedralSunVisiHandler implements EventHandler<FieldOfViewDetector> {

        public Action eventOccurred(final SpacecraftState s, final FieldOfViewDetector detector,
                                    final boolean increasing)
            throws OrekitException {
            if (increasing) {
                //System.err.println(" Sun visibility starts " + s.getDate());
                AbsoluteDate startVisiDate = new AbsoluteDate(new DateComponents(1969, 8, 28),
                                                              new TimeComponents(1, 19, 00.381),
                                                              TimeScalesFactory.getUTC());

                Assert.assertTrue(s.getDate().durationFrom(startVisiDate) <= 1);
                return Action.CONTINUE;
            } else {
                AbsoluteDate endVisiDate = new AbsoluteDate(new DateComponents(1969, 8, 28),
                                                              new TimeComponents(1, 39 , 42.674),
                                                              TimeScalesFactory.getUTC());
                Assert.assertTrue(s.getDate().durationFrom(endVisiDate) <= 1);
                //System.err.println(" Sun visibility ends at " + s.getDate());
                return Action.CONTINUE;//STOP;
            }
        }

    }

}
