package org.orekit.bodies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.apache.commons.math3.util.FastMath;
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
        assertEquals(FastMath.toRadians(45), point.getLatitude(), 0);
        assertEquals(FastMath.toRadians(-90), point.getLongitude(), 0);
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

            // verify to within 5 ulps
            assertEquals(point[2], gp.getLatitude(), 5 * FastMath.ulp(point[2]));
            assertEquals(point[3], gp.getLongitude(),
                    5 * FastMath.ulp(point[3]));
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
        assertEquals(point, new GeodeticPoint(1, 2, 3));
        assertNotEquals(point, new GeodeticPoint(0, 2, 3));
        assertNotEquals(point, new GeodeticPoint(1, 0, 3));
        assertNotEquals(point, new GeodeticPoint(1, 2, 0));
        assertNotEquals(point, new Object());
    }

    /**
     * check {@link GeodeticPoint#hashCode()}.
     */
    @Test
    public void testToString() {
        // setup
        GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(30),
                FastMath.toRadians(60), 90);

        // action
        String actual = point.toString();

        // verify
        assertEquals("{lat: 30 deg, lon: 60 deg, alt: 90}", actual);
    }
}
