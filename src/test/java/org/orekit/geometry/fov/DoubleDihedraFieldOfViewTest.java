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
package org.orekit.geometry.fov;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.UnitSphereRandomVectorGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.geometry.fov.PolygonalFieldOfView.DefiningConeType;
import org.orekit.propagation.events.VisibilityTrigger;

public class DoubleDihedraFieldOfViewTest {

    @Test
    public void testDihedralFielOfView() {
        double maxError = 0;
        for (double alpha1 = 0; alpha1 < 0.5 * FastMath.PI; alpha1 += 0.1) {
            for (double alpha2 = 0; alpha2 < 0.5 * FastMath.PI; alpha2 += 0.1) {
                DoubleDihedraFieldOfView fov = new DoubleDihedraFieldOfView(Vector3D.PLUS_I,
                                                                            Vector3D.PLUS_K, alpha1,
                                                                            Vector3D.PLUS_J, alpha2,
                                                                            0.125);
                double eta = FastMath.acos(FastMath.sin(alpha1) * FastMath.sin(alpha2));
                double theoreticalArea = MathUtils.TWO_PI - 4 * eta;
                double error = theoreticalArea - fov.getZone().getSize();
                maxError = FastMath.max(FastMath.abs(error), maxError);
                Assertions.assertEquals(0.125, fov.getMargin(), 1.0e-15);
            }
        }
        Assertions.assertEquals(0, maxError, 4.0e-15);
    }

    @Test
    public void testTooWideDihedralFielOfView() {
        double tooLarge = 1.6;
        try {
            new DoubleDihedraFieldOfView(Vector3D.PLUS_I,
                                         Vector3D.PLUS_K, 0.1,
                                         Vector3D.PLUS_J, tooLarge,
                                         0.125);
            Assertions.fail("an exception should have been thrown");
        } catch(OrekitException oe) {
            Assertions.assertEquals(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, oe.getSpecifier());
            Assertions.assertEquals(tooLarge,          (Double) oe.getParts()[0], 1.0e-15);
            Assertions.assertEquals(0,                 (Double) oe.getParts()[1], 1.0e-15);
            Assertions.assertEquals(0.5 * FastMath.PI, (Double) oe.getParts()[2], 1.0e-15);
        }
    }

    @Test
    public void testSquare() {
        DoubleDihedraFieldOfView square1 = new DoubleDihedraFieldOfView(Vector3D.PLUS_K,
                                                                        Vector3D.PLUS_I, 0.25,
                                                                        Vector3D.MINUS_J, 0.25,
                                                                        0.0);
        PolygonalFieldOfView square2 = new PolygonalFieldOfView(Vector3D.PLUS_K, DefiningConeType.INSIDE_CONE_TOUCHING_POLYGON_AT_EDGES_MIDDLE,
                                                                Vector3D.PLUS_I, 0.25, 4, 0.0);
        Assertions.assertEquals(square1.getZone().getSize(),         square2.getZone().getSize(),         1.0e-15);
        Assertions.assertEquals(square1.getZone().getBoundarySize(), square2.getZone().getBoundarySize(), 1.0e-15);
        UnitSphereRandomVectorGenerator random =
                        new UnitSphereRandomVectorGenerator(3, new Well1024a(0x17df21c7598b114bl));
        for (int i = 0; i < 1000; ++i) {
            Vector3D v = new Vector3D(random.nextVector()).scalarMultiply(1.0e6);
            Assertions.assertEquals(square1.offsetFromBoundary(v, 0.125, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV),
                                square2.offsetFromBoundary(v, 0.125, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV),
                                2.6e-15);
        }
    }

}
