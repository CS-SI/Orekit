/* Copyright 2002-2008 CS Communication & Systèmes
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

import static org.junit.Assert.assertEquals;

import org.apache.commons.math.geometry.Vector3D;
import org.junit.Test;
import org.orekit.frames.FrameFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TAIScale;
import org.orekit.time.TimeComponents;

public class SphericalSpacecraftTest {

    @Test
    public void testConstructor() {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                                TimeComponents.H00,
                                TAIScale.getInstance());

        // Satellite position as circular parameters
        final double mu = 3.9860047e14;
        final double raan = 270.;
        Orbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(raan),
                                   Math.toRadians(5.300 - raan), CircularOrbit.MEAN_LONGITUDE_ARGUMENT,
                                   FrameFactory.getEME2000(), date, mu);
        SpacecraftState state = new SpacecraftState(circ);
        SphericalSpacecraft s = new SphericalSpacecraft(1.0, 2.0, 3.0, 4.0);
        Vector3D[] directions = { Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_K };
        for (int i = 0; i < directions.length; ++i) {
            assertEquals(1.0, s.getDragCrossSection(state, directions[i]), 1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getDragCoef(state, directions[i]),
                                      2.0, directions[i]).getNorm(),
                         1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getAbsorptionCoef(state, directions[i]),
                                      3.0, directions[i]).getNorm(),
                         1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getReflectionCoef(state, directions[i]),
                                      4.0, directions[i]).getNorm(),
                         1.0e-15);
        }
    }

    @Test
    public void testSettersGetters() {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                                             TimeComponents.H00,
                                             TAIScale.getInstance());

                     // Satellite position as circular parameters
                     final double mu = 3.9860047e14;
                     final double raan = 270.;
                     Orbit circ =
                         new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(raan),
                                                Math.toRadians(5.300 - raan), CircularOrbit.MEAN_LONGITUDE_ARGUMENT,
                                                FrameFactory.getEME2000(), date, mu);
                     SpacecraftState state = new SpacecraftState(circ);
        SphericalSpacecraft s = new SphericalSpacecraft(0, 0, 0, 0);
        s.setCrossSection(1.0);
        s.setDragCoeff(2.0);
        s.setAbsorptionCoeff(3.0);
        s.setReflectionCoeff(4.0);
        Vector3D[] directions = { Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_K };
        for (int i = 0; i < directions.length; ++i) {
            assertEquals(1.0, s.getDragCrossSection(state, directions[i]), 1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getDragCoef(state, directions[i]),
                                      2.0, directions[i]).getNorm(),
                         1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getAbsorptionCoef(state, directions[i]),
                                      3.0, directions[i]).getNorm(),
                         1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getReflectionCoef(state, directions[i]),
                                      4.0, directions[i]).getNorm(),
                         1.0e-15);
        }
    }

}
