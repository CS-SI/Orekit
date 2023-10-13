/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.spherical.twod.Edge;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.orekit.geometry.fov.CircularFieldOfView;
import org.orekit.geometry.fov.DoubleDihedraFieldOfView;
import org.orekit.geometry.fov.EllipticalFieldOfView;
import org.orekit.geometry.fov.FieldOfView;
import org.orekit.geometry.fov.PolygonalFieldOfView;
import org.orekit.geometry.fov.PolygonalFieldOfView.DefiningConeType;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.EventsLogger.LoggedEvent;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

import java.util.ArrayList;
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

    // UTC time scale
    private TimeScale utc;

    @Test
    public void testDihedralFielOfView() {

        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------

        // Extrapolator definition
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, earthCenterAttitudeLaw);

        // Event definition : square field of view, along X axis, aperture 68°
        final double halfAperture = FastMath.toRadians(0.5 * 68.0);
        final double maxCheck  = 60.;
        final double threshold = 1.0e-10;
        final PVCoordinatesProvider sunPV = CelestialBodyFactory.getSun();
        final Vector3D center = Vector3D.PLUS_I;
        final Vector3D axis1  = Vector3D.PLUS_K;
        final Vector3D axis2  = Vector3D.PLUS_J;
        final double aperture1 = halfAperture;
        final double aperture2 = halfAperture;

        final EventDetector sunVisi =
                new FieldOfViewDetector(sunPV, new DoubleDihedraFieldOfView(center, axis1, aperture1, axis2, aperture2, 0.0)).
                withMaxCheck(maxCheck).
                withThreshold(threshold).
                withHandler(new DihedralSunVisiHandler());

        Assertions.assertSame(sunPV, ((FieldOfViewDetector) sunVisi).getPVTarget());
        Assertions.assertEquals(0, ((FieldOfViewDetector) sunVisi).getFOV().getMargin(), 1.0e-15);
        double eta = FastMath.acos(FastMath.sin(aperture1) * FastMath.sin(aperture2));
        double theoreticalArea = MathUtils.TWO_PI - 4 * eta;
        Assertions.assertEquals(theoreticalArea,
                            ((PolygonalFieldOfView) ((FieldOfViewDetector) sunVisi).getFOV()).getZone().getSize(),
                            1.0e-15);

        // Add event to be detected
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(sunVisi));

        // Extrapolate from the initial to the final date
        propagator.propagate(initDate.shiftedBy(6000.));

        // Sun is in dihedra 1 between tB and tC and in dihedra1 between tA and tD
        // dihedra 1 is entered and left from same side (dihedra angle increases from < -34° to about -31.6° max
        // and then decreases again to < -34°)
        // dihedra 2 is completely crossed (dihedra angle increases from < -34° to > +34°
        final AbsoluteDate tA = new AbsoluteDate("1969-08-28T00:04:50.540686", utc);
        final AbsoluteDate tB = new AbsoluteDate("1969-08-28T00:08:08.299196", utc);
        final AbsoluteDate tC = new AbsoluteDate("1969-08-28T00:29:58.478894", utc);
        final AbsoluteDate tD = new AbsoluteDate("1969-08-28T00:36:13.390275", utc);

        List<LoggedEvent>  events = logger.getLoggedEvents();
        final AbsoluteDate t0     = events.get(0).getState().getDate();
        final AbsoluteDate t1     = events.get(1).getState().getDate();
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(0, t0.durationFrom(tB), 1.0e-6);
        Assertions.assertEquals(0, t1.durationFrom(tC), 1.0e-6);

        for (double dt = 0; dt < 3600; dt += 10.0) {
            AbsoluteDate t = initialOrbit.getDate().shiftedBy(dt);
            double[] angles = dihedralAngles(center, axis1, axis2,
                                             sunPV.getPVCoordinates(t, initialOrbit.getFrame()),
                                             new KeplerianPropagator(initialOrbit, earthCenterAttitudeLaw).propagate(t));
            if (t.compareTo(tA) < 0) {
                // before tA, we are outside of both dihedras
                Assertions.assertTrue(angles[0] < -halfAperture);
                Assertions.assertTrue(angles[1] < -halfAperture);
            } else if (t.compareTo(tB) < 0) {
                // between tA and tB, we are inside dihedra 2 but still outside of dihedra 1
                Assertions.assertTrue(angles[0] < -halfAperture);
                Assertions.assertTrue(angles[1] > -halfAperture);
                Assertions.assertTrue(angles[1] < +halfAperture);
            } else if (t.compareTo(tC) < 0) {
                // between tB and tC, we are inside both dihedra 1 and dihedra 2
                Assertions.assertTrue(angles[0] > -halfAperture);
                Assertions.assertTrue(angles[0] < +halfAperture);
                Assertions.assertTrue(angles[1] > -halfAperture);
                Assertions.assertTrue(angles[1] < +halfAperture);
            } else if (t.compareTo(tD) < 0) {
                // between tC and tD, we are inside dihedra 2 but again outside of dihedra 1
                Assertions.assertTrue(angles[0] < -halfAperture);
                Assertions.assertTrue(angles[1] > -halfAperture);
                Assertions.assertTrue(angles[1] < +halfAperture);
            } else {
                // after tD, we are outside of both dihedras
                Assertions.assertTrue(angles[0] < -halfAperture);
                Assertions.assertTrue(angles[1] > +halfAperture);
            }
        }

    }

    private double[] dihedralAngles(final Vector3D center, final Vector3D axis1, final Vector3D axis2,
                                    final PVCoordinates target, final SpacecraftState s) {
        final Rotation toInert     = s.getAttitude().getOrientation().getRotation().revert();
        final Vector3D centerInert = toInert.applyTo(center);
        final Vector3D axis1Inert  = toInert.applyTo(axis1);
        final Vector3D axis2Inert  = toInert.applyTo(axis2);
        final Vector3D direction   = target.getPosition().subtract(s.getPosition()).normalize();
        return new double[] {
            dihedralAngle(centerInert, axis1Inert, direction),
            dihedralAngle(centerInert, axis2Inert, direction)
        };
    }

    private double dihedralAngle(final Vector3D center, final Vector3D axis, final Vector3D u) {
        final Vector3D y = Vector3D.crossProduct(axis, center).normalize();
        final Vector3D x = Vector3D.crossProduct(y, axis).normalize();
        return FastMath.atan2(Vector3D.dotProduct(u, y), Vector3D.dotProduct(u, x));
    }

    /** check the default behavior to stop propagation on FoV exit. */
    @Test
    public void testStopOnExit() {
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
                PositionAngleType.TRUE, eci, date, Constants.EGM96_EARTH_MU);
        AttitudeProvider attitude = new NadirPointing(eci, earth);

        //action
        FieldOfView fov =
                new PolygonalFieldOfView(Vector3D.PLUS_K,
                                         DefiningConeType.INSIDE_CONE_TOUCHING_POLYGON_AT_EDGES_MIDDLE,
                                         Vector3D.PLUS_I, pi / 3, 16, 0);
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
        Assertions.assertEquals(2, actual.size());
    }

    @Test
    public void testRadius() {

        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------

        // Extrapolator definition
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, earthCenterAttitudeLaw);

        // Event definition : square field of view, along X axis, aperture 68°
        final double halfAperture = FastMath.toRadians(0.5 * 68.0);
        final double maxCheck  = 60.;
        final double threshold = 1.0e-10;
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final Vector3D center = Vector3D.PLUS_I;
        final Vector3D axis1  = Vector3D.PLUS_K;
        final Vector3D axis2  = Vector3D.PLUS_J;
        final double aperture1 = halfAperture;
        final double aperture2 = halfAperture;
        final FieldOfView fov  = new DoubleDihedraFieldOfView(center, axis1, aperture1, axis2, aperture2, 0.0);

        final EventDetector sunCenter =
                        new FieldOfViewDetector(sun, fov).
                        withMaxCheck(maxCheck).
                        withThreshold(threshold).
                        withHandler(new ContinueOnEvent());

        final EventDetector sunFull =
                        new FieldOfViewDetector(sun, Constants.SUN_RADIUS,
                                                VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV,
                                                fov).
                        withMaxCheck(maxCheck).
                        withThreshold(threshold).
                        withHandler(new ContinueOnEvent());

        final EventDetector sunPartial =
                        new FieldOfViewDetector(sun, Constants.SUN_RADIUS,
                                                VisibilityTrigger.VISIBLE_AS_SOON_AS_PARTIALLY_IN_FOV,
                                                fov).
                        withMaxCheck(maxCheck).
                        withThreshold(threshold).
                        withHandler(new ContinueOnEvent());

        Assertions.assertSame(sun, ((FieldOfViewDetector) sunCenter).getPVTarget());
        Assertions.assertEquals(0, ((FieldOfViewDetector) sunCenter).getFOV().getMargin(), 1.0e-15);
        double eta = FastMath.acos(FastMath.sin(aperture1) * FastMath.sin(aperture2));
        double theoreticalArea = MathUtils.TWO_PI - 4 * eta;
        Assertions.assertEquals(theoreticalArea,
                            ((PolygonalFieldOfView) ((FieldOfViewDetector) sunCenter).getFOV()).getZone().getSize(),
                            1.0e-15);

        // Add event to be detected
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(sunCenter));
        propagator.addEventDetector(logger.monitorDetector(sunFull));
        propagator.addEventDetector(logger.monitorDetector(sunPartial));

        // Extrapolate from the initial to the final date
        propagator.propagate(initDate.shiftedBy(6000.));

        List<LoggedEvent>  events = logger.getLoggedEvents();
        Assertions.assertEquals(6, events.size());
        Assertions.assertSame(sunPartial, events.get(0).getEventDetector());
        Assertions.assertEquals(460.876793, events.get(0).getState().getDate().durationFrom(initialOrbit.getDate()), 1.0e-6);
        Assertions.assertSame(sunCenter, events.get(1).getEventDetector());
        Assertions.assertEquals(488.299210, events.get(1).getState().getDate().durationFrom(initialOrbit.getDate()), 1.0e-6);
        Assertions.assertSame(sunFull, events.get(2).getEventDetector());
        Assertions.assertEquals(517.536353, events.get(2).getState().getDate().durationFrom(initialOrbit.getDate()), 1.0e-6);
        Assertions.assertSame(sunFull, events.get(3).getEventDetector());
        Assertions.assertEquals(1749.277930, events.get(3).getState().getDate().durationFrom(initialOrbit.getDate()), 1.0e-6);
        Assertions.assertSame(sunCenter, events.get(4).getEventDetector());
        Assertions.assertEquals(1798.478948, events.get(4).getState().getDate().durationFrom(initialOrbit.getDate()), 1.0e-6);
        Assertions.assertSame(sunPartial, events.get(5).getEventDetector());
        Assertions.assertEquals(1845.979622, events.get(5).getState().getDate().durationFrom(initialOrbit.getDate()), 1.0e-6);

    }

    @Test
    public void testMatryoshka() {

        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------

        // Extrapolator definition
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, earthCenterAttitudeLaw);

        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final double maxCheck  = 60.;
        final double threshold = 1.0e-10;
        EventsLogger logger = new EventsLogger();

        // largest fov: circular, along X axis, aperture 68°, no margin
        CircularFieldOfView circFov = new CircularFieldOfView(Vector3D.PLUS_I, FastMath.toRadians(0.5 * 68.0), 0.0);
        List<EventDetector> detectors = new ArrayList<>();
        for (int i = 0; i < 4; ++i) {

            // outer circular detector
            final EventDetector circDetector =
                            new FieldOfViewDetector(sun, circFov).
                            withMaxCheck(maxCheck).
                            withThreshold(threshold).
                            withHandler(new ContinueOnEvent());
            detectors.add(circDetector);
            propagator.addEventDetector(logger.monitorDetector(circDetector));

            // inner polygonal detector
            PolygonalFieldOfView polyFov = new PolygonalFieldOfView(circFov.getCenter(),
                                                                    DefiningConeType.OUTSIDE_CONE_TOUCHING_POLYGON_AT_VERTICES,
                                                                    circFov.getCenter().orthogonal(),
                                                                    circFov.getHalfAperture(), 16, 0.0);
            final EventDetector polyDetector =
                            new FieldOfViewDetector(sun, polyFov).
                            withMaxCheck(maxCheck).
                            withThreshold(threshold).
                            withHandler(new ContinueOnEvent());
            detectors.add(polyDetector);
            propagator.addEventDetector(logger.monitorDetector(polyDetector));

            // find another inner circular fov
            final Edge     edge   = polyFov.getZone().getBoundaryLoops().get(0).getOutgoing();
            final Vector3D middle = edge.getPointAt(0.5 * edge.getLength());
            final double   innerRadius = Vector3D.angle(circFov.getCenter(), middle);
            circFov = new CircularFieldOfView(circFov.getCenter(), innerRadius, 0.0);

        }

        // Extrapolate from the initial to the final date
        propagator.propagate(initDate.shiftedBy(6000.));

        int n = detectors.size();
        List<LoggedEvent>  events = logger.getLoggedEvents();
        Assertions.assertEquals(2 * n, events.size());

        // series of Sun visibility start events, from outer to inner FoV
        for (int i = 0; i < n; ++i) {
            Assertions.assertSame(detectors.get(i), events.get(i).getEventDetector());
        }

        // series of Sun visibility end events, from inner to outer FoV
        for (int i = 0; i < n; ++i) {
            Assertions.assertSame(detectors.get(n - 1 - i), events.get(n + i).getEventDetector());
        }

    }

    @Test
    public void testElliptical() {

        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------

        // Extrapolator definition
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, earthCenterAttitudeLaw);

        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final double maxCheck  = 60.;
        final double threshold = 1.0e-10;
        EventsLogger logger = new EventsLogger();

        EllipticalFieldOfView fov = new EllipticalFieldOfView(Vector3D.PLUS_I, Vector3D.PLUS_J,
                                                              FastMath.toRadians(40), FastMath.toRadians(10),
                                                              0.0);
        propagator.addEventDetector(logger.monitorDetector(new FieldOfViewDetector(sun, fov).
                                                           withMaxCheck(maxCheck).
                                                           withThreshold(threshold).
                                                           withHandler(new ContinueOnEvent())));

       // Extrapolate from the initial to the final date
        propagator.propagate(initDate.shiftedBy(6000.));

        List<LoggedEvent>  events = logger.getLoggedEvents();
        Assertions.assertEquals(2, events.size());

        Assertions.assertFalse(events.get(0).isIncreasing());
        Assertions.assertEquals(881.897, events.get(0).getState().getDate().durationFrom(initDate), 1.0e-3);
        Assertions.assertTrue(events.get(1).isIncreasing());
        Assertions.assertEquals(1242.146, events.get(1).getState().getDate().durationFrom(initDate), 1.0e-3);

    }

    @BeforeEach
    public void setUp() {
        try {

            Utils.setDataRoot("regular-data");

            utc = TimeScalesFactory.getUTC();

            // Computation date
            // Satellite position as circular parameters
            mu = 3.9860047e14;

            initDate = new AbsoluteDate(new DateComponents(1969, 8, 28), TimeComponents.H00, utc);

            Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
            Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
            initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                FramesFactory.getEME2000(), initDate, mu);


            // WGS84 Earth model
            earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                         Constants.WGS84_EARTH_FLATTENING,
                                         FramesFactory.getITRF(IERSConventions.IERS_2010, true));

            // Create earth center pointing attitude provider
            earthCenterAttitudeLaw = new BodyCenterPointing(initialOrbit.getFrame(), earth);

        } catch (OrekitException oe) {
            Assertions.fail(oe.getMessage());
        }

    }


    /** Handler for visibility event. */
    private static class DihedralSunVisiHandler implements EventHandler {

        public Action eventOccurred(final SpacecraftState s, final EventDetector detector,
                                    final boolean increasing) {
            if (increasing) {
                //System.err.println(" Sun visibility starts " + s.getDate());
                AbsoluteDate startVisiDate = new AbsoluteDate(new DateComponents(1969, 8, 28),
                                                              new TimeComponents(1, 19, 00.381),
                                                              TimeScalesFactory.getUTC());

                Assertions.assertTrue(s.getDate().durationFrom(startVisiDate) <= 1);
                return Action.CONTINUE;
            } else {
                AbsoluteDate endVisiDate = new AbsoluteDate(new DateComponents(1969, 8, 28),
                                                              new TimeComponents(1, 39 , 42.674),
                                                              TimeScalesFactory.getUTC());
                Assertions.assertTrue(s.getDate().durationFrom(endVisiDate) <= 1);
                //System.err.println(" Sun visibility ends at " + s.getDate());
                return Action.CONTINUE;//STOP;
            }
        }

    }

}
