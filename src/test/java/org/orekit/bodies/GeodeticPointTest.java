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

import org.junit.Assert;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Test;

/**
 * Unit tests for {@link GeodeticPoint}.
 *
 * @author Evan Ward
 *
 */
public class GeodeticPointTest {

    /**
     * check {@link GeodeticPoint#GeodeticPoint(double, double, double)} angle
     * normalization.
     */
    @Test
    public void testGeodeticPointAngleNormalization() {
        // action
        GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(135),
                FastMath.toRadians(90 - 360), 0);

        // verify
        Assert.assertEquals(FastMath.toRadians(45), point.getLatitude(), 0);
        Assert.assertEquals(FastMath.toRadians(-90), point.getLongitude(), 0);

        Assert.assertEquals(0, Vector3D.distance(point.getEast(),   Vector3D.PLUS_I), 1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(point.getNorth(),  new Vector3D( 0.50 * FastMath.PI,  0.25 * FastMath.PI)), 1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(point.getWest(),   Vector3D.MINUS_I), 1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(point.getSouth(),  new Vector3D(-0.50 * FastMath.PI, -0.25 * FastMath.PI)), 1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(point.getZenith(), new Vector3D(-0.50 * FastMath.PI,  0.25 * FastMath.PI)), 1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(point.getNadir(),  new Vector3D( 0.50 * FastMath.PI, -0.25 * FastMath.PI)), 1.0e-15);

    }

    /**
     * check {@link GeodeticPoint#GeodeticPoint(double, double, double)} for
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
            GeodeticPoint gp = new GeodeticPoint(point[0], point[1], 0);
            Assert.assertEquals(0, gp.getEast().crossProduct(gp.getNorth()).distance(gp.getZenith()), 1.0e-15);
            Assert.assertEquals(0, gp.getNorth().crossProduct(gp.getWest()).distance(gp.getZenith()), 1.0e-15);
            Assert.assertEquals(0, gp.getSouth().crossProduct(gp.getWest()).distance(gp.getNadir()), 1.0e-15);
            Assert.assertEquals(0, gp.getEast().crossProduct(gp.getSouth()).distance(gp.getNadir()), 1.0e-15);
            Assert.assertEquals(0, gp.getZenith().crossProduct(gp.getSouth()).distance(gp.getEast()), 1.0e-15);
            Assert.assertEquals(0, gp.getNadir().crossProduct(gp.getWest()).distance(gp.getNorth()), 1.0e-15);

            // verify to within 5 ulps
            Assert.assertEquals(point[2], gp.getLatitude(), 5 * FastMath.ulp(point[2]));
            Assert.assertEquals(point[3], gp.getLongitude(), 5 * FastMath.ulp(point[3]));
        }
    }

    /**
     * check {@link GeodeticPoint#equals(Object)}.
     */
    @Test
    public void testEquals() {
        // setup
        GeodeticPoint point = new GeodeticPoint(1, 2, 3);

        // actions + verify
        Assert.assertEquals(point, new GeodeticPoint(1, 2, 3));
        Assert.assertFalse(point.equals(new GeodeticPoint(0, 2, 3)));
        Assert.assertFalse(point.equals(new GeodeticPoint(1, 0, 3)));
        Assert.assertFalse(point.equals(new GeodeticPoint(1, 2, 0)));
        Assert.assertFalse(point.equals(new Object()));
        Assert.assertEquals(point.hashCode(), new GeodeticPoint(1, 2, 3).hashCode());
        Assert.assertNotEquals(point.hashCode(), new GeodeticPoint(1, FastMath.nextUp(2), 3).hashCode());
    }

    /**
     * check {@link GeodeticPoint#toString()}.
     */
    @Test
    public void testToString() {
        // setup
        GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(30),
                FastMath.toRadians(60), 90);

        // action
        String actual = point.toString();

        // verify
        Assert.assertEquals("{lat: 30 deg, lon: 60 deg, alt: 90}", actual);
    }
}
