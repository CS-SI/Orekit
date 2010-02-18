/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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


import org.apache.commons.math.geometry.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;

public class BoxAndSolarArraySpacecraftTest {

    @Test
    public void testNormal() throws OrekitException {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J, 1.0, 2.0, 0.5);

        for (double dt = 0; dt < 4000; dt += 60) {

            SpacecraftState state = propagator.propagate(initialDate.shiftedBy(dt));

            Vector3D sunDirInert = sun.getPVCoordinates(initialDate, state.getFrame()).getPosition();
            Vector3D momentum = state.getPVCoordinates().getMomentum();
            double sunElevation = Math.PI / 2 - Vector3D.angle(sunDirInert, momentum);
            Assert.assertEquals(15.1, Math.toDegrees(sunElevation), 0.1);

            Vector3D n = s.getNormal(state);
            Assert.assertEquals(0.0, n.getY(), 1.0e-10);

            // normal misalignment should be entirely due to sun being out of orbital plane
            Vector3D sunDirSat = state.getAttitude().getRotation().applyTo(sunDirInert);
            double misAlignment = Vector3D.angle(sunDirSat, n);
            Assert.assertEquals(sunElevation, misAlignment, 1.0e-3);

        }
    }

    @Test
    public void testCoefficients() throws OrekitException {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        BoxAndSolarArraySpacecraft s =
            new BoxAndSolarArraySpacecraft(1.5, 3.5, 2.5, sun, 20.0, Vector3D.PLUS_J, 1.0, 2.0, 0.5);

        double minSx = Double.POSITIVE_INFINITY;
        double maxSx = Double.NEGATIVE_INFINITY;
        double minSy = Double.POSITIVE_INFINITY;
        double maxSy = Double.NEGATIVE_INFINITY;
        double minSz = Double.POSITIVE_INFINITY;
        double maxSz = Double.NEGATIVE_INFINITY;
        Vector3D dummy = new Vector3D(0.1, 0.2, 0.3).normalize();
        for (double dt = 0; dt < 4000; dt += 2.0) {

            SpacecraftState state = propagator.propagate(initialDate.shiftedBy(dt));

            double sx = s.getDragCrossSection(state, Vector3D.PLUS_I);
            Assert.assertEquals(sx, s.getRadiationCrossSection(state, Vector3D.PLUS_I), 1.0e-10);
            minSx = Math.min(minSx, sx);
            maxSx = Math.max(maxSx, sx);
            double sy = s.getDragCrossSection(state, Vector3D.PLUS_J);
            Assert.assertEquals(sy, s.getRadiationCrossSection(state, Vector3D.PLUS_J), 1.0e-10);
            minSy = Math.min(minSy, sy);
            maxSy = Math.max(maxSy, sy);
            double sz = s.getDragCrossSection(state, Vector3D.PLUS_K);
            Assert.assertEquals(sz, s.getRadiationCrossSection(state, Vector3D.PLUS_K), 1.0e-10);
            minSz = Math.min(minSz, sz);
            maxSz = Math.max(maxSz, sz);
            Assert.assertEquals(0, Vector3D.angle(dummy, s.getAbsorptionCoef(state, dummy)), 1.0e-10);
            Assert.assertEquals(0, Vector3D.angle(dummy, s.getReflectionCoef(state, dummy)), 1.0e-10);
            Assert.assertEquals(0, Vector3D.angle(dummy, s.getDragCoef(state, dummy)), 1.0e-10);
        }

        // expected cross section along X is 3.5m * 2.5m for body + 20m^2 * |cos(alpha)|
        // expected cross section along Y is 1.5m * 2.5m for body
        // expected cross section along Z is 1.5m * 3.5m for body + 20m^2 * |sin(alpha)|
        Assert.assertEquals(3.5 * 2.5,      minSx, 0.02);
        Assert.assertEquals(3.5 * 2.5 + 20, maxSx, 0.02);
        Assert.assertEquals(1.5 * 2.5,      minSy, 0.02);
        Assert.assertEquals(1.5 * 2.5,      maxSy, 0.02);
        Assert.assertEquals(1.5 * 3.5,      minSz, 0.02);
        Assert.assertEquals(1.5 * 3.5 + 20, maxSz, 0.02);
        
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
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(280),
                                   Math.toRadians(10.0), CircularOrbit.MEAN_LONGITUDE_ARGUMENT,
                                   FramesFactory.getEME2000(), date, mu);
        propagator =
            new EcksteinHechlerPropagator(circ, LofOffset.LOF_ALIGNED, ae, mu, c20, c30, c40, c50, c60);
        } catch (OrekitException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    private double mu;
    private Propagator propagator;

}
