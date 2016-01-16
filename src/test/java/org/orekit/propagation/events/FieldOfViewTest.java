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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.spherical.twod.S2Point;
import org.apache.commons.math3.geometry.spherical.twod.SphericalPolygonsSet;
import org.apache.commons.math3.random.UnitSphereRandomVectorGenerator;
import org.apache.commons.math3.random.Well1024a;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;

public class FieldOfViewTest {

    @Test
    public void testDihedralFielOfView() throws OrekitException {
        double maxError = 0;
        for (double alpha1 = 0; alpha1 < 0.5 * FastMath.PI; alpha1 += 0.1) {
            for (double alpha2 = 0; alpha2 < 0.5 * FastMath.PI; alpha2 += 0.1) {
                FieldOfView fov = new FieldOfView(Vector3D.PLUS_I,
                                                  Vector3D.PLUS_K, alpha1,
                                                  Vector3D.PLUS_J, alpha2,
                                                  0.125);
                double eta = FastMath.acos(FastMath.sin(alpha1) * FastMath.sin(alpha2));
                double theoreticalArea = MathUtils.TWO_PI - 4 * eta;
                double error = theoreticalArea - fov.getZone().getSize();
                maxError = FastMath.max(FastMath.abs(error), maxError);
                Assert.assertEquals(0.125, fov.getMargin(), 1.0e-15);
            }
        }
        Assert.assertEquals(0, maxError, 4.0e-15);
    }

    @Test
    public void testTooWideDihedralFielOfView() throws OrekitException {
        double tooLarge = 1.6;
        try {
            new FieldOfView(Vector3D.PLUS_I,
                            Vector3D.PLUS_K, 0.1,
                            Vector3D.PLUS_J, tooLarge,
                            0.125);
            Assert.fail("an exception should have been thrown");
        } catch(OrekitException oe) {
            Assert.assertEquals(LocalizedFormats.NUMBER_TOO_LARGE, oe.getSpecifier());
            Assert.assertEquals(tooLarge, (Double) oe.getParts()[0], 1.0e-15);
            Assert.assertEquals(0.5 * FastMath.PI, (Double) oe.getParts()[1], 1.0e-15);
        }
    }

    @Test
    public void testSquare() throws OrekitException {
        FieldOfView square1 = new FieldOfView(Vector3D.PLUS_K,
                                              Vector3D.PLUS_I, 0.25,
                                              Vector3D.MINUS_J, 0.25,
                                              0.0);
        FieldOfView square2 = new FieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I, 0.25, 4, 0.0);
        Assert.assertEquals(square1.getZone().getSize(),         square2.getZone().getSize(),         1.0e-15);
        Assert.assertEquals(square1.getZone().getBoundarySize(), square2.getZone().getBoundarySize(), 1.0e-15);
        UnitSphereRandomVectorGenerator random =
                        new UnitSphereRandomVectorGenerator(3, new Well1024a(0x17df21c7598b114bl));
        for (int i = 0; i < 1000; ++i) {
            Vector3D v = new Vector3D(random.nextVector()).scalarMultiply(1.0e6);
            Assert.assertEquals(square1.offsetFromBoundary(v), square2.offsetFromBoundary(v), 1.0e-15);
        }
    }

    @Test
    public void testRegularPolygon() throws OrekitException {
        double delta          = 0.25;
        double margin         = 0.01;
        double maxAreaError   = 0;
        double maxOffsetError = 0;
        for (int n = 3; n < 32; ++n) {
            FieldOfView fov = new FieldOfView(Vector3D.PLUS_K, Vector3D.PLUS_I, delta, n, margin);
            double eta = FastMath.acos(FastMath.sin(FastMath.PI / n) * FastMath.cos(delta));
            double theoreticalArea = 2 * n * eta - (n - 2) * FastMath.PI;
            double areaError = theoreticalArea - fov.getZone().getSize();
            maxAreaError = FastMath.max(FastMath.abs(areaError), maxAreaError);
            for (double lambda = -0.5 * FastMath.PI; lambda < 0.5 * FastMath.PI; lambda += 0.1) {
                Vector3D v = new Vector3D(0.0, lambda).scalarMultiply(1.0e6);
                double theoreticalOffset = 0.5 * FastMath.PI - lambda - delta - margin;
                double offset = fov.offsetFromBoundary(v);
                if (theoreticalOffset > 0.01) {
                    // the offsetFromBoundary method may use the fast approximate
                    // method, so we cannot check the error accurately
                    // we know however that the fast method will underestimate the offset
                    
                    Assert.assertTrue(offset > 0);
                    Assert.assertTrue(offset <= theoreticalOffset + 5e-16);
                } else {
                    double offsetError = theoreticalOffset - offset;
                    maxOffsetError = FastMath.max(FastMath.abs(offsetError), maxOffsetError);
                }
            }
        }
        Assert.assertEquals(0.0, maxAreaError,   5.0e-14);
        Assert.assertEquals(0.0, maxOffsetError, 2.0e-15);
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        FieldOfView fov = new FieldOfView(new SphericalPolygonsSet(1.0e-12,
                                                                   new S2Point(Vector3D.PLUS_I),
                                                                   new S2Point(Vector3D.PLUS_J),
                                                                   new S2Point(Vector3D.PLUS_K)),
                                          0.001);
        Assert.assertEquals(0.5 * FastMath.PI, fov.getZone().getSize(),         1.0e-15);
        Assert.assertEquals(1.5 * FastMath.PI, fov.getZone().getBoundarySize(), 1.0e-15);
        Assert.assertEquals(0.001,  fov.getMargin(), 1.0e-15);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(fov);

        
        Assert.assertTrue(bos.size() > 400);
        Assert.assertTrue(bos.size() < 450);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        FieldOfView deserialized  = (FieldOfView) ois.readObject();
        Assert.assertEquals(0.5 * FastMath.PI, deserialized.getZone().getSize(),         1.0e-15);
        Assert.assertEquals(1.5 * FastMath.PI, deserialized.getZone().getBoundarySize(), 1.0e-15);
        Assert.assertEquals(0.001,  deserialized.getMargin(), 1.0e-15);
        
    }

}
