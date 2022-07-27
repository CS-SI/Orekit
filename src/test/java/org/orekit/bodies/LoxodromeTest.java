/* Copyright 2002-2022 Joseph Reed
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Joseph Reed licenses this file to You under the Apache License, Version 2.0
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** Unit tests for {@link Loxodrome} and {@link LoxodromeArc}. */
public class LoxodromeTest {
    private GeodeticPoint newYork;
    private GeodeticPoint london;
    private GeodeticPoint berlin;
    private GeodeticPoint perth;
    private GeodeticPoint philadelphia;
    private OneAxisEllipsoid earth;

    @Before
    public void setup() {
        Utils.setDataRoot("regular-data");

        newYork = new GeodeticPoint(FastMath.toRadians(40.714268), FastMath.toRadians(-74.005974), 0);
        london = new GeodeticPoint(FastMath.toRadians(51.5), FastMath.toRadians(-0.16667), 0);
        berlin = new GeodeticPoint(FastMath.toRadians(52.523403), FastMath.toRadians(13.4114), 0);
        perth = new GeodeticPoint(FastMath.toRadians(-31.952712), FastMath.toRadians(115.8604796), 0);
        philadelphia = new GeodeticPoint(FastMath.toRadians(39.952330), FastMath.toRadians(-75.16379), 0);

        final Frame ecef = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        earth  = new OneAxisEllipsoid(6378137, 1. / 298.257223563, ecef);
    }

    /** Verify short distance.
     */
    @Test
    public void verifyShortDistance() {
        // short distance: new york - philadelphia
        executeTest("new york - philadelphia", newYork, philadelphia, FastMath.toRadians(229.31), 1., 5.);
    }

    @Test
    public void verifyLongDistance() {
        executeTest("new york - london", newYork, london, FastMath.toRadians(78.09), 30., 6_000.);
        executeTest("berlin - perth", berlin, perth, FastMath.toRadians(132.89), 90., 35_000.);
    }

    void executeTest(final String header, final GeodeticPoint start, final GeodeticPoint stop, final double expectedAzimuth, final double numericalError, final double pointError) {
        final double az = earth.azimuthBetweenPoints(start, stop);
        Assert.assertEquals(expectedAzimuth, az, 1e-4);

        final Loxodrome lox = new Loxodrome(start, az, earth);
        final LoxodromeArc arc = new LoxodromeArc(start, stop, earth);

        final GeodeticPoint expected = highFidelityPointAtDistance(start, az, arc.getDistance());
        final GeodeticPoint actual = lox.pointAtDistance(arc.getDistance());

        final double error1 = earth.transform(actual).distance(earth.transform(expected));
        final double error2 = earth.transform(actual).distance(earth.transform(stop));

        // over short distances, analytic answer within 1m of numerical computation
        Assert.assertTrue(header + " analytic answer not within " + numericalError + "m of numerical answer [error=" + error1 + "m]", error1 < numericalError);
        // over short distances, analytic answer is within 5m of actual lat/lon
        Assert.assertTrue(header + " computed destination not within " + pointError + "m of actual answer.[error=" + error2 + "m]", error2 < pointError);
        // verify accuracy to 0.001%
        Assert.assertEquals(header + " accuracy beyond allowable tolerance", 0.0, error1 / arc.getDistance(), 1e-5);
    }

    GeodeticPoint highFidelityPointAtDistance(final GeodeticPoint start, final double azimuth, final double distance) {
        GeodeticPoint point = start;
        double d = distance;
        final Vector3D step = new Vector3D(FastMath.sin(azimuth), FastMath.cos(azimuth), 0);
        while (d > 0) {
            final GeodeticPoint tmp = earth.transform(
                new TopocentricFrame(earth, point, "frame").getTransformTo(earth.getBodyFrame(), AbsoluteDate.ARBITRARY_EPOCH).transformPosition(step),
                earth.getBodyFrame(),
                AbsoluteDate.ARBITRARY_EPOCH);
            d -= 1.;
            point = tmp;
        }
        return point;
    }
}
