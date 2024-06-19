/* Copyright 2002-2024 Thales Alenia Space
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

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

/**
 * Unit tests for {@link SexagesimalAngle}.
 *
 * @author Luc Maisonobe
 *
 */
public class SexagesimalAngleTest {

    @Test
    public void testZeroDMS() {
        Assertions.assertEquals(0.0, new SexagesimalAngle(1, 0, 0, 0.0).getAngle(), 1.0e-15);
    }

    @Test
    public void testZeroRadians() {
        final SexagesimalAngle angle = new SexagesimalAngle(0.0);
        Assertions.assertEquals(1,   angle.getSign());
        Assertions.assertEquals(0,   angle.getDegree());
        Assertions.assertEquals(0,   angle.getArcMinute());
        Assertions.assertEquals(0.0, angle.getArcSecond(), 1.0e-15);
    }

    @Test
    public void testPositiveRighAngleDMS() {
        Assertions.assertEquals(MathUtils.SEMI_PI, new SexagesimalAngle(1, 90, 0, 0.0).getAngle(), 1.0e-15);
    }

    @Test
    public void testPositiveRighAngleRadians() {
        final SexagesimalAngle angle = new SexagesimalAngle(MathUtils.SEMI_PI);
        Assertions.assertEquals( 1,   angle.getSign());
        Assertions.assertEquals(90,   angle.getDegree());
        Assertions.assertEquals( 0,   angle.getArcMinute());
        Assertions.assertEquals( 0.0, angle.getArcSecond(), 1.0e-15);
    }

    @Test
    public void testNegativeRighAngleDMS() {
        Assertions.assertEquals(-MathUtils.SEMI_PI, new SexagesimalAngle(-1, 90, 0, 0.0).getAngle(), 1.0e-15);
    }

    @Test
    public void testNegativeRighAngleRadians() {
        final SexagesimalAngle angle = new SexagesimalAngle(-MathUtils.SEMI_PI);
        Assertions.assertEquals(-1,   angle.getSign());
        Assertions.assertEquals(90,   angle.getDegree());
        Assertions.assertEquals( 0,   angle.getArcMinute());
        Assertions.assertEquals( 0.0, angle.getArcSecond(), 1.0e-15);
    }

    @Test
    public void testIter() {
        final String[] expected = new String[] {
            "00W 10′ 15.0″", "00W 09′ 45.0″", "00W 09′ 15.0″", "00W 08′ 45.0″", "00W 08′ 15.0″", "00W 07′ 45.0″",
            "00W 07′ 15.0″", "00W 06′ 45.0″", "00W 06′ 15.0″", "00W 05′ 45.0″", "00W 05′ 15.0″", "00W 04′ 45.0″",
            "00W 04′ 15.0″", "00W 03′ 45.0″", "00W 03′ 15.0″", "00W 02′ 45.0″", "00W 02′ 15.0″", "00W 01′ 45.0″",
            "00W 01′ 15.0″", "00W 00′ 45.0″", "00W 00′ 15.0″", "00E 00′ 15.0″", "00E 00′ 45.0″", "00E 01′ 15.0″",
            "00E 01′ 45.0″", "00E 02′ 15.0″", "00E 02′ 45.0″", "00E 03′ 15.0″", "00E 03′ 45.0″", "00E 04′ 15.0″",
            "00E 04′ 45.0″", "00E 05′ 15.0″", "00E 05′ 45.0″", "00E 06′ 15.0″", "00E 06′ 45.0″", "00E 07′ 15.0″",
            "00E 07′ 45.0″", "00E 08′ 15.0″", "00E 08′ 45.0″", "00E 09′ 15.0″", "00E 09′ 45.0″", "00E 10′ 15.0″"
        };

        for (int i = 0; i < expected.length; i++) {
            final SexagesimalAngle angle = new SexagesimalAngle(FastMath.toRadians(30.0 / 3600.0) * (i - 20.5));
            final String formatted = String.format(Locale.US, "%02d%c %02d′ %04.1f″",
                                                   angle.getDegree(),
                                                   angle.getSign() < 0 ? 'W' : 'E',
                                                   angle.getArcMinute(),
                                                   angle.getArcSecond());
            Assertions.assertEquals(expected[i], formatted);
        }

    }

}
