/* Contributed in the public domain.
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
package org.orekit.bodies;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for {@link FieldGeodeticPoint}.
 *
 * @author Evan Ward
 *
 */
class FieldGeodeticPointTest {

    /**
     * check {@link FieldGeodeticPoint#FieldGeodeticPoint(CalculusFieldElement, CalculusFieldElement, CalculusFieldElement)} angle
     * normalization.
     */
    @Test
    void testGeodeticPointAngleNormalization() {
        // action
        FieldGeodeticPoint<Binary64> point =
                new FieldGeodeticPoint<>(new Binary64(FastMath.toRadians(135)),
                                         new Binary64(FastMath.toRadians(90 - 360)),
                                         new Binary64(0));

        // verify
        assertEquals(FastMath.toRadians(45), point.getLatitude().getReal(), 1.0e-15);
        assertEquals(FastMath.toRadians(-90), point.getLongitude().getReal(), 1.0e-15);

        assertEquals(0, Vector3D.distance(point.getEast().toVector3D(),   Vector3D.PLUS_I), 1.0e-15);
        assertEquals(0, Vector3D.distance(point.getNorth().toVector3D(),  new Vector3D( 0.50 * FastMath.PI,  0.25 * FastMath.PI)), 1.0e-15);
        assertEquals(0, Vector3D.distance(point.getWest().toVector3D(),   Vector3D.MINUS_I), 1.0e-15);
        assertEquals(0, Vector3D.distance(point.getSouth().toVector3D(),  new Vector3D(-0.50 * FastMath.PI, -0.25 * FastMath.PI)), 1.0e-15);
        assertEquals(0, Vector3D.distance(point.getZenith().toVector3D(), new Vector3D(-0.50 * FastMath.PI,  0.25 * FastMath.PI)), 1.0e-15);
        assertEquals(0, Vector3D.distance(point.getNadir().toVector3D(),  new Vector3D( 0.50 * FastMath.PI, -0.25 * FastMath.PI)), 1.0e-15);

    }

    /**
     * check {@link FieldGeodeticPoint#FieldGeodeticPoint(CalculusFieldElement, CalculusFieldElement, CalculusFieldElement)} for
     * several different angles.
     */
    @Test
    void testGeodeticPoint() {
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
            FieldGeodeticPoint<Binary64> gp =
                    new FieldGeodeticPoint<>(new Binary64(point[0]),
                                             new Binary64(point[1]),
                                             Binary64.ZERO);
            assertEquals(0, gp.getEast().crossProduct(gp.getNorth()).distance(gp.getZenith()).getReal(), 1.0e-15);
            assertEquals(0, gp.getNorth().crossProduct(gp.getWest()).distance(gp.getZenith()).getReal(), 1.0e-15);
            assertEquals(0, gp.getSouth().crossProduct(gp.getWest()).distance(gp.getNadir()).getReal(), 1.0e-15);
            assertEquals(0, gp.getEast().crossProduct(gp.getSouth()).distance(gp.getNadir()).getReal(), 1.0e-15);
            assertEquals(0, gp.getZenith().crossProduct(gp.getSouth()).distance(gp.getEast()).getReal(), 1.0e-15);
            assertEquals(0, gp.getNadir().crossProduct(gp.getWest()).distance(gp.getNorth()).getReal(), 1.0e-15);

            // verify to within 5 ulps
            assertEquals(point[2], gp.getLatitude().getReal(), 5 * FastMath.ulp(point[2]));
            assertEquals(point[3], gp.getLongitude().getReal(), 5 * FastMath.ulp(point[3]));
        }
    }

    /**
     * check {@link FieldGeodeticPoint#equals(Object)}.
     */
    @Test
    void testEquals() {
        // setup
        FieldGeodeticPoint<Binary64> point =
                new FieldGeodeticPoint<>(new Binary64(1),
                                         new Binary64(2),
                                         new Binary64(3));

        // actions + verify
        assertEquals(point, new FieldGeodeticPoint<>(Binary64Field.getInstance(),
                                                                new GeodeticPoint(1, 2, 3)));
        assertNotEquals(point, new FieldGeodeticPoint<>(new Binary64(0), new Binary64(2), new Binary64(3)));
        assertNotEquals(point, new FieldGeodeticPoint<>(new Binary64(1), new Binary64(0), new Binary64(3)));
        assertNotEquals(point,new FieldGeodeticPoint<>(new Binary64(1), new Binary64(2), new Binary64(0)));
        assertNotEquals(point, new Object());
        assertEquals(point.hashCode(),
                                new FieldGeodeticPoint<>(new Binary64(1),
                                                         new Binary64(2),
                                                         new Binary64(3)).hashCode());
        assertNotEquals(point.hashCode(),
                                   new FieldGeodeticPoint<>(new Binary64(1),
                                                            new Binary64(FastMath.nextUp(2)),
                                                            new Binary64(3)).hashCode());
    }

    /**
     * check {@link FieldGeodeticPoint#toString()}.
     */
    @Test
    void testToString() {
        // setup
        FieldGeodeticPoint<Binary64> point =
                new FieldGeodeticPoint<>(new Binary64(FastMath.toRadians(30)),
                                         new Binary64(FastMath.toRadians(60)),
                                         new Binary64(90));

        // action
        String actual = point.toString();

        // verify
        assertEquals("{lat: 30 deg, lon: 60 deg, alt: 90}", actual);
    }

}
