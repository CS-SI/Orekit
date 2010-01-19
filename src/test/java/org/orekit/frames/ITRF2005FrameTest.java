/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.frames;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class ITRF2005FrameTest {

    @Test
    public void testTidalEffects() throws OrekitException {

        final Frame itrfWith    = FramesFactory.getITRF2005(false);
        final Frame itrfWithout = FramesFactory.getITRF2005(true);
        final AbsoluteDate date0 = new AbsoluteDate(2007, 10, 20, TimeScalesFactory.getUTC());

        double minCorrection = Double.POSITIVE_INFINITY;
        double maxCorrection = Double.NEGATIVE_INFINITY;
        for (double dt = 0; dt < 3 * 86400; dt += 60) {
            final AbsoluteDate date = new AbsoluteDate(date0, dt);
            final Transform t = itrfWith.getTransformTo(itrfWithout, date);
            Assert.assertEquals(0, t.getTranslation().getNorm(), 1.0e-15);
            final double milliarcSeconds = Math.toDegrees(t.getRotation().getAngle()) * 3600000.0;
            minCorrection = Math.min(minCorrection, milliarcSeconds);
            maxCorrection = Math.max(maxCorrection, milliarcSeconds);
        }

        Assert.assertEquals(0.064, minCorrection, 0.001);
        Assert.assertEquals(0.613, maxCorrection, 0.001);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
