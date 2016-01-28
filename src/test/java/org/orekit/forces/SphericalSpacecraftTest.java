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


import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;

@Deprecated
public class SphericalSpacecraftTest {

    @Test
    public void testDrag() throws OrekitException {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                                TimeComponents.H00,
                                TimeScalesFactory.getTAI());

        // Satellite position as circular parameters
        final double mu = 3.9860047e14;
        final double raan = 270.;
        Orbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(raan),
                                   FastMath.toRadians(5.300 - raan), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        SpacecraftState state = new SpacecraftState(circ);
        double surface = 5.0;
        double cd      = 2.0;
        SphericalSpacecraft s = new SphericalSpacecraft(surface, cd, 0.0, 0.0);
        Vector3D relativeVelocity = new Vector3D(36.0, 48.0, 80.0);

        double rho = 0.001;
        Vector3D computedAcceleration = s.dragAcceleration(state.getDate(), state.getFrame(),
                                                           state.getPVCoordinates().getPosition(),
                                                           state.getAttitude().getRotation(),
                                                           state.getMass(), rho, relativeVelocity);
        Vector3D d = relativeVelocity.normalize();
        double v2 = relativeVelocity.getNormSq();
        Vector3D expectedAcceleration = new Vector3D(rho * surface * cd * v2 / (2 * state.getMass()), d);
        Assert.assertEquals(0.0, computedAcceleration.subtract(expectedAcceleration).getNorm(), 1.0e-15);

    }

    @Test
    public void testRadiationPressure() throws OrekitException {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                                TimeComponents.H00,
                                TimeScalesFactory.getTAI());

        // Satellite position as circular parameters
        final double mu = 3.9860047e14;
        final double raan = 270.;
        Orbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(raan),
                                   FastMath.toRadians(5.300 - raan), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        SpacecraftState state = new SpacecraftState(circ);
        double surface = 5.0;
        double kA      = 0.9;
        double kR      = 0.1;
        SphericalSpacecraft s = new SphericalSpacecraft(surface, 0.0, kA, kR);
        Vector3D flux = new Vector3D(36.0, 48.0, 80.0);

        Vector3D computedAcceleration =
                s.radiationPressureAcceleration(state.getDate(), state.getFrame(),
                                                state.getPVCoordinates().getPosition(),
                                                state.getAttitude().getRotation(),
                                                state.getMass(), flux);
        Vector3D d = flux.normalize();
        double f = flux.getNorm();
        double p = (1 - kA) * (1 - kR);
        Vector3D expectedAcceleration = new Vector3D(surface * f * (1 + 4 * p / 9) / state.getMass(), d);
        Assert.assertEquals(0.0, computedAcceleration.subtract(expectedAcceleration).getNorm(), 1.0e-15);

    }

}
