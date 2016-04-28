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
package org.orekit.models.earth.tessellation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.orekit.OrekitMatchers.geodeticPointCloseTo;

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.tessellation.Tile;

public class TileTest {

    @Test
    public void testCenteredSquare() {
        double angle = 0.25;
        GeodeticPoint v0 = new GeodeticPoint(-angle, -angle, 100.0);
        GeodeticPoint v1 = new GeodeticPoint(-angle, +angle, 100.0);
        GeodeticPoint v2 = new GeodeticPoint(+angle, -angle, 100.0);
        GeodeticPoint v3 = new GeodeticPoint(+angle, +angle, 100.0);
        Tile tile = new Tile(v0, v1, v2, v3);
        assertThat(tile.getVertices()[0], geodeticPointCloseTo(v0, 1.0e-9));
        assertThat(tile.getVertices()[1], geodeticPointCloseTo(v1, 1.0e-9));
        assertThat(tile.getVertices()[2], geodeticPointCloseTo(v2, 1.0e-9));
        assertThat(tile.getVertices()[3], geodeticPointCloseTo(v3, 1.0e-9));
        assertThat(tile.getVertices()[3], geodeticPointCloseTo(v3, 1.0e-9));

        assertThat(tile.getInterpolatedPoint(0, 0), geodeticPointCloseTo(v0, 1.0e-9));
        assertThat(tile.getInterpolatedPoint(1, 0), geodeticPointCloseTo(v1, 1.0e-9));
        assertThat(tile.getInterpolatedPoint(1, 1), geodeticPointCloseTo(v2, 1.0e-9));
        assertThat(tile.getInterpolatedPoint(0, 1), geodeticPointCloseTo(v3, 1.0e-9));

        assertThat(tile.getCenter(),
                   geodeticPointCloseTo(new GeodeticPoint(0.0, 0.0, 100.0), 1.0e-9));

    }

    @Test
    public void testPoleCentered() {
        double latitude = 0.25;
        GeodeticPoint v0 = new GeodeticPoint(latitude, 0.0 * FastMath.PI, 100.0);
        GeodeticPoint v1 = new GeodeticPoint(latitude, 0.5 * FastMath.PI, 200.0);
        GeodeticPoint v2 = new GeodeticPoint(latitude, 1.0 * FastMath.PI, 300.0);
        GeodeticPoint v3 = new GeodeticPoint(latitude, 1.5 * FastMath.PI, 200.0);
        Tile tile = new Tile(v0, v1, v2, v3);
        assertThat(tile.getVertices()[0], geodeticPointCloseTo(v0, 1.0e-9));
        assertThat(tile.getVertices()[1], geodeticPointCloseTo(v1, 1.0e-9));
        assertThat(tile.getVertices()[2], geodeticPointCloseTo(v2, 1.0e-9));
        assertThat(tile.getVertices()[3], geodeticPointCloseTo(v3, 1.0e-9));
        assertThat(tile.getVertices()[3], geodeticPointCloseTo(v3, 1.0e-9));

        assertThat(tile.getInterpolatedPoint(0, 0), geodeticPointCloseTo(v0, 1.0e-9));
        assertThat(tile.getInterpolatedPoint(1, 0), geodeticPointCloseTo(v1, 1.0e-9));
        assertThat(tile.getInterpolatedPoint(1, 1), geodeticPointCloseTo(v2, 1.0e-9));
        assertThat(tile.getInterpolatedPoint(0, 1), geodeticPointCloseTo(v3, 1.0e-9));

        Assert.assertEquals(0.5 * FastMath.PI, tile.getCenter().getLatitude(), 1.0e-9);

    }

}
