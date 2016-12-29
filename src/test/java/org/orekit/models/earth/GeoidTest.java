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
package org.orekit.models.earth;

import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.EGMFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.orekit.OrekitMatchers.closeTo;
import static org.orekit.OrekitMatchers.geodeticPointCloseTo;
import static org.orekit.OrekitMatchers.vectorCloseTo;

/**
 * Unit tests for {@link Geoid}.
 *
 * @author Evan Ward
 */
public class GeoidTest {

    /** maximum degree and order used in testing {@link Geoid}. */
    @SuppressWarnings("javadoc")
    private static final int maxOrder = 360, maxDegree = 360;
    /** The WGS84 reference ellipsoid. */
    private static ReferenceEllipsoid WGS84 = new ReferenceEllipsoid(
            6378137.00, 1 / 298.257223563, FramesFactory.getGCRF(),
            3.986004418e14, 7292115e-11);
    /**
     * The potential to use in {@link #getComponent()}. Set in {@link
     * #setUpBefore()}.
     */
    private static NormalizedSphericalHarmonicsProvider potential;
    /** date to use in test cases */
    private static AbsoluteDate date;

    /**
     * load orekit data and gravity field.
     *
     * @throws Exception on error.
     */
    @BeforeClass
    public static void setUpBefore() throws Exception {
        Utils.setDataRoot("geoid:regular-data");
        GravityFieldFactory.clearPotentialCoefficientsReaders();
        GravityFieldFactory.addPotentialCoefficientsReader(
                new EGMFormatReader("egm96", false));
        potential = GravityFieldFactory.getConstantNormalizedProvider(
                maxDegree, maxOrder);
        date = potential.getReferenceDate();
    }

    /** {lat, lon, expectedValue} points to evaluate the undulation */
    private double[][] reference;

    /** create the array of reference points */
    @Before
    public void setUp() {
        reference = new double[][]{
                {0, 75, -100.3168},
                {-30, 60, 14.9214},
                {0, 130, 76.6053},
                {60, -30, 63.7979},
                {40, -75, -34.0402},
                {28, 92, -30.4056},// the Himalayas
                {45, 250, -7.4825},// the rockies
                // this section is taken from the NGA's test file
                {38.6281550, 269.7791550, -31.628},
                {-14.6212170, 305.0211140, -2.969},
                {46.8743190, 102.4487290, -43.575},
                {-23.6174460, 133.8747120, 15.871},
                {38.6254730, 359.9995000, 50.066},
                {-.4667440, .0023000, 17.329}};
    }

    /**
     * Gets a new instance of {@link Geoid} to test with. It is given the EGM96
     * potential and the WGS84 ellipsoid.
     *
     * @return a new {@link Geoid}
     */
    private Geoid getComponent() {
        return new Geoid(potential, WGS84);
    }

    /** Test constructor and simple getters. */
    @Test
    public void testGeoid() {
        Geoid geoid = getComponent();
        // reference ellipse is the same
        assertEquals(WGS84, geoid.getEllipsoid());
        // geoid and reference ellipse are in the same frame
        assertEquals(WGS84.getBodyFrame(), geoid.getBodyFrame());
    }

    /** throws on null */
    @Test(expected = NullPointerException.class)
    public void testGeoidNullPotential() {
        new Geoid(null, WGS84);
    }

    /** throws on null */
    @Test(expected = NullPointerException.class)
    public void testGeoidNullEllipsoid() {
        new Geoid(potential, null);
    }

    /**
     * Test several pre-computed points from the Online Geoid Height Evaluation
     * tool, which takes into account terrain.
     *
     * @throws OrekitException on error
     * @see <a href="http://geographiclib.sourceforge.net/cgi-bin/GeoidEval">Online
     * Geoid Height Evaluation</a>
     * @see <a href="http://earth-info.nga.mil/GandG/wgs84/gravitymod/egm96/egm96.html">Geoid
     * height for WGS84 and EGM96</a>
     */
    @Test
    public void testGetUndulation() throws OrekitException {
        /*
         * allow 3 meter of error, which is what the approximations would
         * suggest, see the comment for Geoid.
         */
        final double maxError = 3;

        // run the test
        Geoid geoid = getComponent();
        for (double[] row : reference) {
            double lat = row[0];
            double lon = row[1];
            double undulation = geoid.getUndulation(FastMath.toRadians(lat),
                    FastMath.toRadians(lon), date);
            double expected = row[2];
            // System.out.format("%10g %10g %10g %10g%n", lat, lon, expected,
            // undulation - expected);
            Assert.assertEquals(String.format("lat: %5g, lon: %5g", lat, lon),
                    undulation, expected, maxError);
        }
    }

    /**
     * check {@link Geoid#getIntersectionPoint(Line, Vector3D, Frame,
     * AbsoluteDate)} with several points.
     *
     * @throws OrekitException on error
     */
    @Test
    public void testGetIntersectionPoint() throws OrekitException {
        // setup
        Geoid geoid = getComponent();
        Frame frame = geoid.getBodyFrame();

        for (double[] point : reference) {
            GeodeticPoint gp = new GeodeticPoint(FastMath.toRadians(point[0]),
                    FastMath.toRadians(point[1]), 0);
            Vector3D expected = geoid.transform(gp);
            // glancing line: 10% vertical and 90% north (~6 deg elevation)
            Vector3D slope = gp.getZenith().scalarMultiply(0.1)
                    .add(gp.getNorth().scalarMultiply(0.9));
            Vector3D close = expected.add(slope.scalarMultiply(100e3));
            Vector3D pointOnLine = expected.add(slope);
            Line line = new Line(close, pointOnLine, 0);
            // line directed the other way
            Line otherDirection = new Line(pointOnLine, close, 0);

            // action
            GeodeticPoint actual = geoid.getIntersectionPoint(line, close,
                    frame, date);
            // other direction
            GeodeticPoint actualReversed = geoid.getIntersectionPoint(
                    otherDirection, close, frame, date);

            // verify
            String message = String.format("point: %s%n",
                    Arrays.toString(point));
            // position accuracy on Earth's surface to 1.3 um.
            assertThat(message, actualReversed, geodeticPointCloseTo(gp, 1.3e-6));
            assertThat(message, actual, geodeticPointCloseTo(gp, 1.3e-6));
        }
    }

    /**
     * check {@link Geoid#getIntersectionPoint(Line, Vector3D, Frame,
     * AbsoluteDate)} handles frame transformations correctly
     *
     * @throws OrekitException on error
     */
    @Test
    public void testGetIntersectionPointFrame() throws OrekitException {
        // setup
        Geoid geoid = getComponent();
        Frame frame = new Frame(
                geoid.getBodyFrame(),
                new Transform(
                        date,
                        new Transform(
                                date,
                                new Vector3D(-1, 2, -3),
                                new Vector3D(4, -5, 6)),
                        new Transform(
                                date,
                                new Rotation(-7, 8, -9, 10, true),
                                new Vector3D(-11, 12, -13))),
                "test frame");
        GeodeticPoint gp = new GeodeticPoint(FastMath.toRadians(46.8743190),
                FastMath.toRadians(102.4487290), 0);
        Vector3D expected = geoid.transform(gp);
        // glancing line: 10% vertical and 90% north (~6 deg elevation)
        Vector3D slope = gp.getZenith().scalarMultiply(0.1)
                .add(gp.getNorth().scalarMultiply(0.9));
        Vector3D close = expected.add(slope.scalarMultiply(100));
        Line line = new Line(expected.add(slope), close, 0);
        Transform xform = geoid.getBodyFrame().getTransformTo(frame, date);
        // transform to test frame
        close = xform.transformPosition(close);
        line = xform.transformLine(line);

        // action
        GeodeticPoint actual = geoid.getIntersectionPoint(line, close, frame,
                date);

        // verify, 1 um position accuracy at Earth's surface
        assertThat(actual, geodeticPointCloseTo(gp, 1e-6));
    }

    /**
     * check {@link Geoid#getIntersectionPoint(Line, Vector3D, Frame,
     * AbsoluteDate)} returns null when there is no intersection
     *
     * @throws OrekitException on error
     */
    @Test
    public void testGetIntersectionPointNoIntersection() throws OrekitException {
        Geoid geoid = getComponent();
        Vector3D closeMiss = new Vector3D(geoid.getEllipsoid()
                .getEquatorialRadius() + 18, 0, 0);
        Line line = new Line(closeMiss, closeMiss.add(Vector3D.PLUS_J), 0);

        // action
        final GeodeticPoint actual = geoid.getIntersectionPoint(line,
                closeMiss, geoid.getBodyFrame(), date);

        // verify
        assertThat(actual, nullValue());
    }

    /**
     * check altitude is referenced to the geoid. h<sub>ellipse</sub> =
     * h<sub>geoid</sub> + N. Where N is the undulation of the geoid.
     *
     * @throws OrekitException on error
     */
    @Test
    public void testTransformVector3DFrameAbsoluteDate()
            throws OrekitException {
        // frame and date are the same
        Frame frame = FramesFactory.getGCRF();
        AbsoluteDate date = AbsoluteDate.CCSDS_EPOCH;

        Geoid geoid = getComponent();
        // test point at 0,0,0
        Vector3D point = new Vector3D(WGS84.getEquatorialRadius(), 0, 0);
        double undulation = geoid.getUndulation(0, 0, date);

        // check ellipsoidal points and geoidal points differ by undulation
        GeodeticPoint ellipsoidal = geoid.getEllipsoid().transform(
                point, frame, date);
        GeodeticPoint geoidal = geoid.transform(point, frame, date);
        assertThat(ellipsoidal.getAltitude() - geoidal.getAltitude(),
                is(undulation));

        // check it is the reverse of transform(GeodeticPoint)
        point = new Vector3D(0.5, 0.4, 0.31).scalarMultiply(WGS84
                .getEquatorialRadius());
        Vector3D expected = geoid
                .transform(geoid.transform(point, frame, date));
        // allow 2 upls of error
        assertThat(point, vectorCloseTo(expected, 2));

    }

    /**
     * check that the altitude is referenced to the geoid (includes
     * undulation).
     *
     * @throws OrekitException on error
     */
    @Test
    public void testTransformGeodeticPoint() throws OrekitException {
        // geoid
        Geoid geoid = getComponent();
        // ellipsoid
        ReferenceEllipsoid ellipsoid = geoid.getEllipsoid();
        // point to test with orthometric height
        GeodeticPoint orthometric = new GeodeticPoint(0, 75, 5);
        // undulation at point
        double undulation = geoid.getUndulation(orthometric.getLatitude(),
                orthometric.getLongitude(), date);
        // same point with height referenced to ellipsoid
        GeodeticPoint point = new GeodeticPoint(orthometric.getLatitude(),
                orthometric.getLongitude(), orthometric.getAltitude()
                + undulation);

        // test they are the same
        Vector3D expected = ellipsoid.transform(point);
        Vector3D actual = geoid.transform(orthometric);
        assertThat(actual, is(expected));

        // test the point 0,0,0
        expected = new Vector3D(WGS84.getEquatorialRadius()
                + geoid.getUndulation(0, 0, date), 0, 0);
        actual = geoid.transform(new GeodeticPoint(0, 0, 0));
        assertThat(actual, vectorCloseTo(expected, 0));
    }

    /** check {@link Geoid#getEllipsoid()} */
    @Test
    public void testGetEllipsoid() {
        //setup
        Geoid geoid = new Geoid(potential, WGS84);

        //action + verify
        assertThat(geoid.getEllipsoid(), sameInstance(WGS84));
    }

    /**
     * check {@link Geoid#projectToGround(Vector3D, AbsoluteDate, Frame)}
     *
     * @throws OrekitException on error
     */
    @Test
    public void testProjectToGround() throws OrekitException {
        //setup
        Vector3D p = new Vector3D(7e8, 1e3, 200);
        Geoid geoid = new Geoid(potential, WGS84);

        //action
        Vector3D actual = geoid.projectToGround(p, date, FramesFactory.getGCRF());

        //verify
        assertThat(
                geoid.transform(actual, geoid.getBodyFrame(), date).getAltitude(),
                closeTo(0.0, 1.1e-9));
    }

}
