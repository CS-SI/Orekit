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

import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.utils.Constants;

import static org.hipparchus.util.FastMath.PI;
import static org.hipparchus.util.FastMath.abs;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.orekit.OrekitMatchers.relativelyCloseTo;

/**
 * Unit tests for {@link ReferenceEllipsoid}.
 *
 * @author E. Ward
 */
public class ReferenceEllipsoidTest {


    /** Test the constructor and the simple getters. */
    @Test
    public void testReferenceEllipsoid() {
        double a = 1, f = 0.5, gm = 2, omega = 3;
        ReferenceEllipsoid ellipsoid = new ReferenceEllipsoid(a, f,
                FramesFactory.getGCRF(), gm, omega);
        assertThat(ellipsoid.getEquatorialRadius(), is(a));
        assertThat(ellipsoid.getFlattening(), is(f));
        assertThat(ellipsoid.getGM(), is(gm));
        assertThat(ellipsoid.getSpin(), is(omega));
    }

    /**
     * Gets the WGS84 ellipsoid.
     *
     * @return the WGS84 ellipsoid.
     * @see "Department of Defense World Geodetic System 1984. 2000. NIMA TR
     * 8350.2 Third Edition, Amendment 1."
     */
    private ReferenceEllipsoid getComponent() {
        /*
         * use values for WGS84 ellipsoid. From:
         * http://earth-info.nga.mil/GandG/wgs84/gravitymod/egm96/egm96.html
         */
        double a = 6378137.00, f = 1 / 298.257223563, GM = 3.986004418e14, spin = 7292115e-11;
        return new ReferenceEllipsoid(a, f, FramesFactory.getGCRF(), GM, spin);
    }

    /**
     * Check the computation of normal gravity at several latitudes, allow 1
     * ulps of error.
     */
    @Test
    public void testGetNormalGravity() {
        ReferenceEllipsoid ellipsoid = getComponent();

        // latitudes to evaluate at, in degrees
        double[] lat = {-90, -45, 0, 45, 90};
        // expected value of normal gravity at each latitude in lat
        double[] expected = {9.833276738917813685281,
                9.8064684244573317502963, 9.779777598918278489352,
                9.8064684244573317502963, 9.833276738917813685281};

        // run tests
        assertEquals(lat.length, expected.length);
        for (int i = 0; i < expected.length; i++) {
            assertThat(ellipsoid.getNormalGravity(lat[i] * PI / 180.),
                    relativelyCloseTo(expected[i], 1));
        }
    }

    /** Check the J<sub>2</sub> term is correct. */
    @Test
    public void testGetC2n0() {
        ReferenceEllipsoid ellipsoid = getComponent();
        /*
         * C2,0 from: Department of Defense World Geodetic System 1984. 2000.
         * NIMA TR 8350.2 Third Edition, Amendment 1.
         */
        double expected = -0.484166774985e-3;

        // value good to ~ 1e-9
        Assert.assertEquals(
                "J2 term\n", ellipsoid.getC2n0(1), expected, 1.31e-9);

        /*
         * Values from '84, See chapter 3 of DMA TR 8350.2 Table 3.8
         */
        double[] expecteds = {7.90304054e-7, -1.687251e-9, 3.461e-12};
        for (int i = 0; i < expecteds.length; i++) {
            double C2n = ellipsoid.getC2n0(2 + i);
            expected = expecteds[i];
            // expect 4 correct digits
            Assert.assertEquals("C" + (4 + 2 * i) + ",0" + "\n",
                    C2n, expected, abs(2e-4 * expected));
        }
    }

    /** check throws when n=0 */
    @Test(expected = IllegalArgumentException.class)
    public void testGetC2n0Bad() {
        getComponent().getC2n0(0);
    }

    /** check {@link ReferenceEllipsoid#getPolarRadius()} */
    @Test
    public void testGetPolarRadius() {
        assertThat(getComponent().getPolarRadius(), is(6356752.314245179));
    }

    /**
     * check {@link ReferenceEllipsoid#getWgs84(Frame)}
     *
     * @throws OrekitException on error
     */
    @Test
    public void testGetWgs84() throws OrekitException {
        // setup
        double c20factor = GravityFieldFactory.getUnnormalizationFactors(2, 0)[2][0];
        Frame frame = FramesFactory.getGCRF();

        // action

        ReferenceEllipsoid wgs84 = ReferenceEllipsoid.getWgs84(frame);

        // verify
        Assert.assertEquals(
                wgs84.getC2n0(1), Constants.WGS84_EARTH_C20 / c20factor, 3e-9);
        assertThat(wgs84.getBodyFrame(), is(frame));
    }

    /** check {@link ReferenceEllipsoid#getEllipsoid()} */
    @Test
    public void testGetEllipsoid() {
        //setup
        ReferenceEllipsoid ellipsoid = getComponent();

        //action + verify
        assertThat(ellipsoid.getEllipsoid(), is(ellipsoid));
    }
}
