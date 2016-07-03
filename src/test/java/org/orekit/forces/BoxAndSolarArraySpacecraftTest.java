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
package org.orekit.forces;


import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;

public class BoxAndSolarArraySpacecraftTest {

    @Test
    public void testBestPointing() throws OrekitException {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J, 0.0, 0.0, 0.0);
        for (double dt = 0; dt < 4000; dt += 60) {

            SpacecraftState state = propagator.propagate(initialDate.shiftedBy(dt));

            Vector3D sunInert = sun.getPVCoordinates(initialDate, state.getFrame()).getPosition();
            Vector3D momentum = state.getPVCoordinates().getMomentum();
            double sunElevation = FastMath.PI / 2 - Vector3D.angle(sunInert, momentum);
            Assert.assertEquals(15.1, FastMath.toDegrees(sunElevation), 0.1);

            Vector3D n = s.getNormal(state.getDate(), state.getFrame(),
                                     state.getPVCoordinates().getPosition(),
                                     state.getAttitude().getRotation());
            Assert.assertEquals(0.0, n.getY(), 1.0e-10);

            // normal misalignment should be entirely due to sun being out of orbital plane
            Vector3D sunSat = state.getAttitude().getRotation().applyTo(sunInert);
            double misAlignment = Vector3D.angle(sunSat, n);
            Assert.assertEquals(sunElevation, misAlignment, 1.0e-3);

        }
    }

    @Test
    public void testCorrectFixedRate() throws OrekitException {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J,
                                           initialDate,
                                           new Vector3D(0.46565509814462996, 0.0,  0.884966287251619),
                                           propagator.getInitialState().getKeplerianMeanMotion(),
                                           0.0, 0.0, 0.0);

        for (double dt = 0; dt < 4000; dt += 60) {

            SpacecraftState state = propagator.propagate(initialDate.shiftedBy(dt));

            Vector3D sunInert = sun.getPVCoordinates(initialDate, state.getFrame()).getPosition();
            Vector3D momentum = state.getPVCoordinates().getMomentum();
            double sunElevation = FastMath.PI / 2 - Vector3D.angle(sunInert, momentum);
            Assert.assertEquals(15.1, FastMath.toDegrees(sunElevation), 0.1);

            Vector3D n = s.getNormal(state.getDate(), state.getFrame(),
                                     state.getPVCoordinates().getPosition(),
                                     state.getAttitude().getRotation());
            Assert.assertEquals(0.0, n.getY(), 1.0e-10);

            // normal misalignment should be entirely due to sun being out of orbital plane
            Vector3D sunSat = state.getAttitude().getRotation().applyTo(sunInert);
            double misAlignment = Vector3D.angle(sunSat, n);
            Assert.assertEquals(sunElevation, misAlignment, 1.0e-3);

        }
    }

    @Test
        public void testTooSlowFixedRate() throws OrekitException {

            AbsoluteDate initialDate = propagator.getInitialState().getDate();
            CelestialBody sun = CelestialBodyFactory.getSun();
            BoxAndSolarArraySpacecraft s =
                new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J,
                                               initialDate,
                                               new Vector3D(0.46565509814462996, 0.0,  0.884966287251619),
                                               0.1 * propagator.getInitialState().getKeplerianMeanMotion(),
                                               0.0, 0.0, 0.0);

            double maxDelta = 0;
            for (double dt = 0; dt < 4000; dt += 60) {

                SpacecraftState state = propagator.propagate(initialDate.shiftedBy(dt));

                Vector3D sunInert = sun.getPVCoordinates(initialDate, state.getFrame()).getPosition();
                Vector3D momentum = state.getPVCoordinates().getMomentum();
                double sunElevation = FastMath.PI / 2 - Vector3D.angle(sunInert, momentum);
                Assert.assertEquals(15.1, FastMath.toDegrees(sunElevation), 0.1);

                Vector3D n = s.getNormal(state.getDate(), state.getFrame(),
                                         state.getPVCoordinates().getPosition(),
                                         state.getAttitude().getRotation());
                Assert.assertEquals(0.0, n.getY(), 1.0e-10);

                // normal misalignment should become very large as solar array rotation is plain wrong
                Vector3D sunSat = state.getAttitude().getRotation().applyTo(sunInert);
                double misAlignment = Vector3D.angle(sunSat, n);
                maxDelta = FastMath.max(maxDelta, FastMath.abs(sunElevation - misAlignment));

            }
            Assert.assertTrue(FastMath.toDegrees(maxDelta) > 120.0);

    }

    @Test
    public void testWithoutReflection() throws OrekitException {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J, 1.0, 1.0, 0.0);

        Vector3D earthRot = new Vector3D(0.0, 0.0, 7.292115e-4);
        for (double dt = 0; dt < 4000; dt += 60) {

            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);

            // simple Earth fixed atmosphere
            Vector3D p = state.getPVCoordinates().getPosition();
            Vector3D v = state.getPVCoordinates().getVelocity();
            Vector3D vAtm = Vector3D.crossProduct(earthRot, p);
            Vector3D relativeVelocity = vAtm.subtract(v);

            Vector3D drag = s.dragAcceleration(state.getDate(), state.getFrame(),
                                               state.getPVCoordinates().getPosition(),
                                               state.getAttitude().getRotation(),
                                               state.getMass(), 0.001, relativeVelocity);
            Assert.assertEquals(0.0, Vector3D.angle(relativeVelocity, drag), 1.0e-10);

            Vector3D sunDirection = sun.getPVCoordinates(date, state.getFrame()).getPosition().normalize();
            Vector3D flux = new Vector3D(-4.56e-6, sunDirection);
            Vector3D radiation = s.radiationPressureAcceleration(state.getDate(), state.getFrame(),
                                                                 state.getPVCoordinates().getPosition(),
                                                                 state.getAttitude().getRotation(),
                                                                 state.getMass(), flux);
            Assert.assertEquals(0.0, Vector3D.angle(flux, radiation), 1.0e-9);

        }

    }

    @Test
    public void testPlaneSpecularReflection() throws OrekitException {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0, sun, 20.0, Vector3D.PLUS_J, 0.0, 0.0, 1.0);

        for (double dt = 0; dt < 4000; dt += 60) {

            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);

            Vector3D sunDirection = sun.getPVCoordinates(date, state.getFrame()).getPosition().normalize();
            Vector3D flux = new Vector3D(-4.56e-6, sunDirection);
            Vector3D acceleration = s.radiationPressureAcceleration(state.getDate(), state.getFrame(),
                                                                    state.getPVCoordinates().getPosition(),
                                                                    state.getAttitude().getRotation(),
                                                                    state.getMass(), flux);
            Vector3D normal = state.getAttitude().getRotation().applyInverseTo(s.getNormal(state.getDate(), state.getFrame(),
                                                                                           state.getPVCoordinates().getPosition(),
                                                                                           state.getAttitude().getRotation()));

            // solar array normal is slightly misaligned with Sun direction due to Sun being out of orbital plane
            Assert.assertEquals(15.1, FastMath.toDegrees(Vector3D.angle(sunDirection, normal)), 0.11);

            // radiation pressure is exactly opposed to solar array normal as there is only specular reflection
            Assert.assertEquals(180.0, FastMath.toDegrees(Vector3D.angle(acceleration, normal)), 1.0e-3);

        }

    }

    @Test
    public void testPlaneAbsorption() throws OrekitException {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(0, 0, 0, sun, 20.0, Vector3D.PLUS_J, 0.0, 1.0, 0.0);

        for (double dt = 0; dt < 4000; dt += 60) {

            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);

            Vector3D sunDirection = sun.getPVCoordinates(date, state.getFrame()).getPosition().normalize();
            Vector3D flux = new Vector3D(-4.56e-6, sunDirection);
            Vector3D acceleration =
                    s.radiationPressureAcceleration(state.getDate(), state.getFrame(),
                                                    state.getPVCoordinates().getPosition(),
                                                    state.getAttitude().getRotation(),
                                                    state.getMass(), flux);
            Vector3D normal = state.getAttitude().getRotation().applyInverseTo(s.getNormal(state.getDate(), state.getFrame(),
                                                                                           state.getPVCoordinates().getPosition(),
                                                                                           state.getAttitude().getRotation()));

            // solar array normal is slightly misaligned with Sun direction due to Sun being out of orbital plane
            Assert.assertEquals(15.1, FastMath.toDegrees(Vector3D.angle(sunDirection, normal)), 0.11);

            // radiation pressure is exactly opposed to Sun direction as there is only absorption
            Assert.assertEquals(180.0, FastMath.toDegrees(Vector3D.angle(acceleration, sunDirection)), 1.0e-3);

        }

    }

    @Before
    public void setUp() {
        try {
        Utils.setDataRoot("regular-data");
        mu  = 3.9860047e14;
        double ae  = 6.378137e6;
        double c20 = -1.08263e-3;
        double c30 = 2.54e-6;
        double c40 = 1.62e-6;
        double c50 = 2.3e-7;
        double c60 = -5.5e-7;

        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 7, 1),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());

        // Satellite position as circular parameters, raan chosen to have sun elevation with
        // respect to orbit plane roughly evolving roughly from 15 to 15.2 degrees in the test range
        Orbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(280),
                                   FastMath.toRadians(10.0), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
        propagator =
            new EcksteinHechlerPropagator(circ,
                                          new LofOffset(circ.getFrame(), LOFType.VVLH),
                                          ae, mu, c20, c30, c40, c50, c60);
        } catch (OrekitException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    private double mu;
    private Propagator propagator;

}
