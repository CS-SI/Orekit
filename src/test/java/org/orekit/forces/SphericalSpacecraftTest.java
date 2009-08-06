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
import org.orekit.forces.SphericalSpacecraft;

public class SphericalSpacecraftTest {

    @Test
    public void testConstructor() {
        SphericalSpacecraft s = new SphericalSpacecraft(1.0, 2.0, 3.0, 4.0);
        Vector3D[] directions = { Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_K };
        for (int i = 0; i < directions.length; ++i) {
            assertEquals(1.0, s.getDragCrossSection(directions[i]), 1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getDragCoef(directions[i]),
                                      2.0, directions[i]).getNorm(),
                         1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getAbsorptionCoef(directions[i]),
                                      3.0, directions[i]).getNorm(),
                         1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getReflectionCoef(directions[i]),
                                      4.0, directions[i]).getNorm(),
                         1.0e-15);
        }
    }

    @Test
    public void testSettersGetters() {
        SphericalSpacecraft s = new SphericalSpacecraft(0, 0, 0, 0);
        s.setCrossSection(1.0);
        s.setDragCoeff(2.0);
        s.setAbsorptionCoeff(3.0);
        s.setReflectionCoeff(4.0);
        Vector3D[] directions = { Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_K };
        for (int i = 0; i < directions.length; ++i) {
            assertEquals(1.0, s.getDragCrossSection(directions[i]), 1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getDragCoef(directions[i]),
                                      2.0, directions[i]).getNorm(),
                         1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getAbsorptionCoef(directions[i]),
                                      3.0, directions[i]).getNorm(),
                         1.0e-15);
            assertEquals(0.0,
                         new Vector3D(-1, s.getReflectionCoef(directions[i]),
                                      4.0, directions[i]).getNorm(),
                         1.0e-15);
        }
    }

}
