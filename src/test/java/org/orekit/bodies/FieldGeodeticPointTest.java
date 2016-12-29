/* Contributed in the public domain.
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
package org.orekit.bodies;

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link FieldGeodeticPoint}.
 *
 * @author Evan Ward
 *
 */
public class FieldGeodeticPointTest {

    /**
     * check {@link FieldGeodeticPoint#GeodeticPoint(RealFieldElement, RealFieldElement, RealFieldElement)} angle
     * normalization.
     */
    @Test
    public void testGeodeticPointAngleNormalization() {
        // action
        FieldGeodeticPoint<Decimal64> point =
                new FieldGeodeticPoint<Decimal64>(new Decimal64(FastMath.toRadians(135)),
                                                  new Decimal64(FastMath.toRadians(90 - 360)),
                                                  new Decimal64(0));

        // verify
        Assert.assertEquals(FastMath.toRadians(45), point.getLatitude().getReal(), 1.0e-15);
        Assert.assertEquals(FastMath.toRadians(-90), point.getLongitude().getReal(), 1.0e-15);

        Assert.assertEquals(0, Vector3D.distance(point.getEast().toVector3D(),   Vector3D.PLUS_I), 1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(point.getNorth().toVector3D(),  new Vector3D( 0.50 * FastMath.PI,  0.25 * FastMath.PI)), 1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(point.getWest().toVector3D(),   Vector3D.MINUS_I), 1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(point.getSouth().toVector3D(),  new Vector3D(-0.50 * FastMath.PI, -0.25 * FastMath.PI)), 1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(point.getZenith().toVector3D(), new Vector3D(-0.50 * FastMath.PI,  0.25 * FastMath.PI)), 1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(point.getNadir().toVector3D(),  new Vector3D( 0.50 * FastMath.PI, -0.25 * FastMath.PI)), 1.0e-15);

    }

    /**
     * check {@link FieldGeodeticPoint#GeodeticPoint(RealFieldElement, RealFieldElement, RealFieldElement)} for
     * several different angles.
     */
    @Test
    public void testGeodeticPoint() {
        // setup
        // the input and expected results
        final double pi = FastMath.PI;
        double[][] points = {
                // Input lat, Input lon; expected lat, expected lon
                // first quadrant
                { pi / 6, pi / 6, pi / 6, pi / 6 },
                // second quadrant
                { 4 * pi / 6, 4 * pi / 6, pi / 3, -pi / 3 },
                // third quadrant
                { 7 * pi / 6, 7 * pi / 6, -pi / 6, pi / 6 },
                // fourth quadrant
                { -pi / 6, -pi / 6, -pi / 6, -pi / 6 },
                { -4 * pi / 6, -4 * pi / 6, -pi / 3, pi / 3 },
                { -pi / 6, -4 * pi / 3, -pi / 6, 2 * pi / 3 } };

        for (double[] point : points) {
            // action
            FieldGeodeticPoint<Decimal64> gp =
                    new FieldGeodeticPoint<Decimal64>(new Decimal64(point[0]),
                                                      new Decimal64(point[1]),
                                                      Decimal64.ZERO);
            Assert.assertEquals(0, gp.getEast().crossProduct(gp.getNorth()).distance(gp.getZenith()).getReal(), 1.0e-15);
            Assert.assertEquals(0, gp.getNorth().crossProduct(gp.getWest()).distance(gp.getZenith()).getReal(), 1.0e-15);
            Assert.assertEquals(0, gp.getSouth().crossProduct(gp.getWest()).distance(gp.getNadir()).getReal(), 1.0e-15);
            Assert.assertEquals(0, gp.getEast().crossProduct(gp.getSouth()).distance(gp.getNadir()).getReal(), 1.0e-15);
            Assert.assertEquals(0, gp.getZenith().crossProduct(gp.getSouth()).distance(gp.getEast()).getReal(), 1.0e-15);
            Assert.assertEquals(0, gp.getNadir().crossProduct(gp.getWest()).distance(gp.getNorth()).getReal(), 1.0e-15);

            // verify to within 5 ulps
            Assert.assertEquals(point[2], gp.getLatitude().getReal(), 5 * FastMath.ulp(point[2]));
            Assert.assertEquals(point[3], gp.getLongitude().getReal(), 5 * FastMath.ulp(point[3]));
        }
    }

    /**
     * check {@link FieldGeodeticPoint#equals(Object)}.
     */
    @Test
    public void testEquals() {
        // setup
        FieldGeodeticPoint<Decimal64> point =
                new FieldGeodeticPoint<Decimal64>(new Decimal64(1),
                                                  new Decimal64(2),
                                                  new Decimal64(3));

        // actions + verify
        Assert.assertEquals(point, new FieldGeodeticPoint<Decimal64>(new Decimal64(1),
                                                                     new Decimal64(2),
                                                                     new Decimal64(3)));
        Assert.assertFalse(point.equals(new FieldGeodeticPoint<Decimal64>(new Decimal64(0),
                                                                          new Decimal64(2),
                                                                          new Decimal64(3))));
        Assert.assertFalse(point.equals(new FieldGeodeticPoint<Decimal64>(new Decimal64(1),
                                                                          new Decimal64(0),
                                                                          new Decimal64(3))));
        Assert.assertFalse(point.equals(new FieldGeodeticPoint<Decimal64>(new Decimal64(1),
                                                                          new Decimal64(2),
                                                                          new Decimal64(0))));
        Assert.assertFalse(point.equals(new Object()));
        Assert.assertEquals(point.hashCode(),
                            new FieldGeodeticPoint<Decimal64>(new Decimal64(1),
                                                              new Decimal64(2),
                                                              new Decimal64(3)).hashCode());
        Assert.assertNotEquals(point.hashCode(),
                               new FieldGeodeticPoint<Decimal64>(new Decimal64(1),
                                                                 new Decimal64(FastMath.nextUp(2)),
                                                                 new Decimal64(3)).hashCode());
    }

    /**
     * check {@link FieldGeodeticPoint#toString()}.
     */
    @Test
    public void testToString() {
        // setup
        FieldGeodeticPoint<Decimal64> point =
                new FieldGeodeticPoint<Decimal64>(new Decimal64(FastMath.toRadians(30)),
                                                  new Decimal64(FastMath.toRadians(60)),
                                                  new Decimal64(90));

        // action
        String actual = point.toString();

        // verify
        Assert.assertEquals("{lat: 30 deg, lon: 60 deg, alt: 90}", actual);
    }

}
