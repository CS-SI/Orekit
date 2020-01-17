/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hamcrest.MatcherAssert;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

@Deprecated
public class CircularFieldOfViewDetectorTest {

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
    public void testCircularFielOfView() {

        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------

        // Extrapolator definition
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, earthCenterAttitudeLaw);

        // Event definition : circular field of view, along X axis, aperture 35°
        final double maxCheck  = 1.;
        final PVCoordinatesProvider sunPV = CelestialBodyFactory.getSun();
        final Vector3D center = Vector3D.PLUS_I;
        final double aperture = FastMath.toRadians(35);

        final CircularSunVisiHandler handler = new CircularSunVisiHandler(initialOrbit.getDate(),
                                                                          new double[] {
                                                                              667.822332,
                                                                              1518.227375,
                                                                              Double.NaN // never checked
                                                                          });
        final CircularFieldOfViewDetector sunVisi =
            new CircularFieldOfViewDetector(maxCheck, sunPV, center, aperture).
            withThreshold(1.0e-10).
            withHandler(handler);
        Assert.assertEquals(0, Vector3D.distance(center, sunVisi.getCenter()), 1.0e-15);
        Assert.assertEquals(aperture, sunVisi.getHalfAperture(), 1.0e-15);
        Assert.assertSame(sunPV, sunVisi.getPVTarget());

        // Add event to be detected
        propagator.addEventDetector(sunVisi);

        // Extrapolate from the initial to the final date
        propagator.propagate(initDate.shiftedBy(6000.));

    }

    @Test
    public void testRadius() {

        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------

        // Extrapolator definition
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, earthCenterAttitudeLaw);

        // Event definition : circular field of view, along X axis, aperture 35°
        final double maxCheck  = 1.;
        final PVCoordinatesProvider sunPV = CelestialBodyFactory.getSun();
        final Vector3D center = Vector3D.PLUS_I;
        final double aperture = FastMath.toRadians(35);

        final CircularSunVisiHandler handler = new CircularSunVisiHandler(initialOrbit.getDate(),
                                                                          new double[] {
                                                                              653.497633,
                                                                              667.822332,
                                                                              682.723244,
                                                                              1497.118891,
                                                                              1518.227375,
                                                                              1538.812269,
                                                                              Double.NaN // never checked
                                                                          });
        // Add event to be detected
        propagator.addEventDetector(new CircularFieldOfViewDetector(maxCheck, sunPV, center, aperture).
                                    withThreshold(1.0e-10).
                                    withHandler(handler));
        propagator.addEventDetector(new CircularFieldOfViewDetector(maxCheck, sunPV,
                                                                    Constants.SUN_RADIUS,
                                                                    VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV,
                                                                    center, aperture).
                                    withThreshold(1.0e-10).
                                    withHandler(handler));
        propagator.addEventDetector(new CircularFieldOfViewDetector(maxCheck, sunPV,
                                                                    Constants.SUN_RADIUS,
                                                                    VisibilityTrigger.VISIBLE_AS_SOON_AS_PARTIALLY_IN_FOV,
                                                                    center, aperture).
                                    withThreshold(1.0e-10).
                                    withHandler(handler));

        // Extrapolate from the initial to the final date
        propagator.propagate(initDate.shiftedBy(6000.));

    }


    /** Check that the g function is the same as it was in 10.0. */
    @Test
    public void testG() {
        // setup
        Frame frame = FramesFactory.getGCRF();
        AbsoluteDate date = AbsoluteDate.JAVA_EPOCH;
        double gm = Constants.EIGEN5C_EARTH_MU;
        Vector3D targetP = new Vector3D(1, 1, 1);
        PVCoordinatesProvider target =
                (d, f) -> new TimeStampedPVCoordinates(date, targetP, Vector3D.ZERO);
        Attitude attitude = new Attitude(
                date, frame, Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO);
        Vector3D satP = new Vector3D(1, 0, 0);
        SpacecraftState state = new SpacecraftState(
                new CartesianOrbit(
                        new PVCoordinates(satP, new Vector3D(0, 1, 0)),
                        frame, date, gm),
                attitude);
        Vector3D center = Vector3D.PLUS_K;
        double halfAperture = FastMath.PI / 2;

        // action
        double actual = new org.orekit.propagation.events.CircularFieldOfViewDetector(0, target, center, halfAperture).g(state);

        // verify
        MatcherAssert.assertThat(
                actual,
                OrekitMatchers.relativelyCloseTo(FastMath.PI / 4, 1));
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

            // Create earth center pointing attitude provider
            earthCenterAttitudeLaw = new BodyCenterPointing(initialOrbit.getFrame(), earth);

        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }

    }


    /** Handler for visibility event. */
    private static class CircularSunVisiHandler implements EventHandler<EventDetector> {

        private final AbsoluteDate reference;
        private final double[]     expected;
        private       int          count;

        CircularSunVisiHandler(final AbsoluteDate reference, final double[] expected) {
            this.reference = reference;
            this.expected  = expected.clone();
        }

        public void init(final SpacecraftState s, AbsoluteDate target) {
            count = 0;
        }

        public Action eventOccurred(final SpacecraftState s, final EventDetector detector,
                                    final boolean increasing) {
              Assert.assertEquals(expected[count++], s.getDate().durationFrom(reference), 1.0e-6);
              return Action.CONTINUE;
        }

    }

}
