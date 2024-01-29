/* Copyright 2002-2024 Luc Maisonobe
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
package org.orekit.orbits;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

import java.util.List;

public class WalkerConstellationTest {

    @Test
    public void testContainer() {
        final WalkerConstellation w = new WalkerConstellation(60, 15, 1);
        Assertions.assertEquals(60, w.getT());
        Assertions.assertEquals(15, w.getP());
        Assertions.assertEquals( 1, w.getF());
    }
    @Test
    public void testInconsistentPlanes() {
        try {
            new WalkerConstellation(60, 14, 1);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.WALKER_INCONSISTENT_PLANES, oe.getSpecifier());
            Assertions.assertEquals(14, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals(60, ((Integer) oe.getParts()[1]).intValue());
        }
    }

    @Test
    public void testRegularPhasing() {
        final CircularOrbit reference = new CircularOrbit(29600000.0, 1.0e-3, 1.2e-4,
                                                          FastMath.toRadians(56.0), FastMath.toRadians(0),
                                                          FastMath.toRadians(0), PositionAngleType.MEAN,
                                                          FramesFactory.getEME2000(),
                                                          AbsoluteDate.J2000_EPOCH,
                                                          Constants.EIGEN5C_EARTH_MU);
        final WalkerConstellation w = new WalkerConstellation(24, 3, 1);
        final List<List<WalkerConstellationSlot<CircularOrbit>>> all = w.buildRegularSlots(reference);
        Assertions.assertEquals(3, all.size());
        for (int i = 0; i < 3; ++i) {
            final List<WalkerConstellationSlot<CircularOrbit>> l = all.get(i);
            Assertions.assertEquals(8, l.size());
            for (int j = 0; j < 8; ++j) {
                final WalkerConstellationSlot<CircularOrbit> s = l.get(j);
                Assertions.assertSame(w, s.getConstellation());
                Assertions.assertEquals(i, s.getPlane());
                Assertions.assertEquals(j, s.getSatellite(), 1.0e-15);
                final CircularOrbit c = s.getOrbit();
                Assertions.assertEquals(reference.getA(),          c.getA(),          4.0e-8);
                Assertions.assertEquals(reference.getCircularEx(), c.getCircularEx(), 1.0e-15);
                Assertions.assertEquals(reference.getCircularEy(), c.getCircularEy(), 1.0e-15);
                Assertions.assertEquals(FastMath.toDegrees(reference.getI()),
                                        FastMath.toDegrees(c.getI()),
                                        1.0e-14);
                Assertions.assertEquals(i * 120.0,
                                        FastMath.toDegrees(MathUtils.normalizeAngle(c.getRightAscensionOfAscendingNode(),
                                                                                    FastMath.PI)),
                                        9.0e-14);
                Assertions.assertEquals(i * 15.0 + j * 45.0,
                                        FastMath.toDegrees(
                                            MathUtils.normalizeAngle(c.getAlphaM(), FastMath.PI)),
                                        6.0e-14);
            }
        }
    }

    @Test
    public void testInOrbitSpares() {
        final CircularOrbit reference = new CircularOrbit(29600000.0, 1.0e-3, 1.2e-4,
                                                          FastMath.toRadians(56.0), FastMath.toRadians(0),
                                                          FastMath.toRadians(0), PositionAngleType.MEAN,
                                                          FramesFactory.getEME2000(),
                                                          AbsoluteDate.J2000_EPOCH,
                                                          Constants.EIGEN5C_EARTH_MU);
        final WalkerConstellation w = new WalkerConstellation(24, 3, 1);
        final List<List<WalkerConstellationSlot<CircularOrbit>>> regular = w.buildRegularSlots(reference);
        Assertions.assertEquals(3, regular.size());
        final WalkerConstellationSlot<CircularOrbit> slot00 = regular.get(0).get(0);

        final WalkerConstellationSlot<CircularOrbit> spare0 = w.buildSlot(slot00, 0, 4.5);
        Assertions.assertEquals(0,   spare0.getPlane());
        Assertions.assertEquals(4.5, spare0.getSatellite(), 1.0e-15);
        Assertions.assertEquals(reference.getA(),          spare0.getOrbit().getA(),          4.0e-8);
        Assertions.assertEquals(reference.getCircularEx(), spare0.getOrbit().getCircularEx(), 1.0e-15);
        Assertions.assertEquals(reference.getCircularEy(), spare0.getOrbit().getCircularEy(), 1.0e-15);
        Assertions.assertEquals(FastMath.toDegrees(reference.getI()),
                                FastMath.toDegrees(spare0.getOrbit().getI()),
                                1.0e-14);
        Assertions.assertEquals(0.0,
                                FastMath.toDegrees(MathUtils.normalizeAngle(spare0.getOrbit().getRightAscensionOfAscendingNode(),
                                                                            FastMath.PI)),
                                9.0e-14);
        Assertions.assertEquals(202.5,
                                FastMath.toDegrees(MathUtils.normalizeAngle(spare0.getOrbit().getAlphaM(), FastMath.PI)),
                                6.0e-14);

        final WalkerConstellationSlot<CircularOrbit> spare1 = w.buildSlot(slot00, 1, 3.5);
        Assertions.assertEquals(1,   spare1.getPlane());
        Assertions.assertEquals(3.5, spare1.getSatellite(), 1.0e-15);
        Assertions.assertEquals(reference.getA(),          spare1.getOrbit().getA(),          4.0e-8);
        Assertions.assertEquals(reference.getCircularEx(), spare1.getOrbit().getCircularEx(), 1.0e-15);
        Assertions.assertEquals(reference.getCircularEy(), spare1.getOrbit().getCircularEy(), 1.0e-15);
        Assertions.assertEquals(FastMath.toDegrees(reference.getI()),
                                FastMath.toDegrees(spare1.getOrbit().getI()),
                                1.0e-14);
        Assertions.assertEquals(120.0,
                                FastMath.toDegrees(MathUtils.normalizeAngle(spare1.getOrbit().getRightAscensionOfAscendingNode(),
                                                                            FastMath.PI)),
                                9.0e-14);
        Assertions.assertEquals(172.5,
                                FastMath.toDegrees(MathUtils.normalizeAngle(spare1.getOrbit().getAlphaM(), FastMath.PI)),
                                6.0e-14);

        final WalkerConstellationSlot<CircularOrbit> spare2 = w.buildSlot(slot00, 2, 1.5);
        Assertions.assertEquals(2,   spare2.getPlane());
        Assertions.assertEquals(1.5, spare2.getSatellite(), 1.0e-15);
        Assertions.assertEquals(reference.getA(),          spare2.getOrbit().getA(),          4.0e-8);
        Assertions.assertEquals(reference.getCircularEx(), spare2.getOrbit().getCircularEx(), 1.0e-15);
        Assertions.assertEquals(reference.getCircularEy(), spare2.getOrbit().getCircularEy(), 1.0e-15);
        Assertions.assertEquals(FastMath.toDegrees(reference.getI()),
                                FastMath.toDegrees(spare2.getOrbit().getI()),
                                1.0e-14);
        Assertions.assertEquals(240.0,
                                FastMath.toDegrees(MathUtils.normalizeAngle(spare2.getOrbit().getRightAscensionOfAscendingNode(),
                                                                            FastMath.PI)),
                                9.0e-14);
        Assertions.assertEquals(97.5,
                                FastMath.toDegrees(MathUtils.normalizeAngle(spare2.getOrbit().getAlphaM(), FastMath.PI)),
                                6.0e-14);

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
}
