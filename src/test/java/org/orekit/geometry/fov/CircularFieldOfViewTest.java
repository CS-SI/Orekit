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

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.attitudes.LofOffset;
import org.orekit.attitudes.NadirPointing;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;

public class CircularFieldOfViewTest extends AbstractSmoothFieldOfViewTest {

    @Test
    public void testNadirNoMargin() {
        doTestFootprint(new CircularFieldOfView(Vector3D.PLUS_K, FastMath.toRadians(3.0), 0.0),
                        new NadirPointing(orbit.getFrame(), earth),
                        3.0, 3.0, 85.3650, 85.3745, 181027.5, 181028.5);
    }

    @Test
    public void testNadirMargin() {
        doTestFootprint(new CircularFieldOfView(Vector3D.PLUS_K, FastMath.toRadians(3.0), 0.01),
                        new NadirPointing(orbit.getFrame(), earth),
                        3.0, 3.0, 85.3650, 85.3745, 181027.5, 181028.5);
    }

    @Test
    public void testRollPitchYaw() {
        doTestFootprint(new CircularFieldOfView(Vector3D.PLUS_K, FastMath.toRadians(3.0), 0.0),
                        new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS, RotationOrder.XYZ,
                                      FastMath.toRadians(10),
                                      FastMath.toRadians(20),
                                      FastMath.toRadians(5)),
                        3.0, 3.0, 48.8582, 59.4238, 1256410.4, 1761338.4);
    }

    @Test
    public void testFOVPartiallyTruncatedAtLimb() {
        doTestFootprint(new CircularFieldOfView(Vector3D.PLUS_K, FastMath.toRadians(3.0), 0.0),
                        new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS, RotationOrder.XYZ,
                                      FastMath.toRadians(-10),
                                      FastMath.toRadians(-39),
                                      FastMath.toRadians(-5)),
                        0.3899, 3.0, 0.0, 21.7315, 3431325.4, 5346737.5);
    }

    @Test
    public void testFOVLargerThanEarth() {
        doTestFootprint(new CircularFieldOfView(Vector3D.PLUS_K, FastMath.toRadians(45.0), 0.0),
                        new NadirPointing(orbit.getFrame(), earth),
                        40.3505, 40.4655, 0.0, 0.0, 5323032.8, 5347029.8);
    }

    @Test
    public void testFOVAwayFromEarth() {
        doTestFOVAwayFromEarth(new CircularFieldOfView(Vector3D.MINUS_K, FastMath.toRadians(3.0), 0.0),
                               new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS, RotationOrder.XYZ,
                                             FastMath.toRadians(-10),
                                             FastMath.toRadians(-39),
                                             FastMath.toRadians(-5)),
                               Vector3D.MINUS_K);
    }

    @Test
    public void testBoundary() {
        doTestBoundary(new CircularFieldOfView(Vector3D.MINUS_K, FastMath.toRadians(3.0), 0.01),
                       new Well19937a(0x2fdf54d1c6f679afl),
                       2.0e-15);
    }

    @Test
    public void testNoFootprintInside() {
        doTestNoFootprintInside(new CircularFieldOfView(Vector3D.PLUS_K, FastMath.toRadians(3.0), 0.0),
                                new Transform(AbsoluteDate.J2000_EPOCH, new Vector3D(5e6, 3e6, 2e6)));
    }

}
