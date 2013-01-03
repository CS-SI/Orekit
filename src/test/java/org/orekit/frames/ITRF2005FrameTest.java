/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.frames;


import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class ITRF2005FrameTest {

    @Test
    public void testTidalEffects() throws OrekitException {

        final Frame itrfWith    = FramesFactory.getITRF2005(false);
        final Frame itrfWithout = FramesFactory.getITRF2005(true);
        final AbsoluteDate date0 = new AbsoluteDate(2007, 10, 20, TimeScalesFactory.getUTC());

        double minCorrection = Double.POSITIVE_INFINITY;
        double maxCorrection = Double.NEGATIVE_INFINITY;
        for (double dt = 0; dt < 3 * Constants.JULIAN_DAY; dt += 60) {
            final AbsoluteDate date = date0.shiftedBy(dt);
            final Transform t = itrfWith.getTransformTo(itrfWithout, date);
            Assert.assertEquals(0, t.getTranslation().getNorm(), 1.0e-15);
            final double milliarcSeconds = FastMath.toDegrees(t.getRotation().getAngle()) * 3600000.0;
            minCorrection = FastMath.min(minCorrection, milliarcSeconds);
            maxCorrection = FastMath.max(maxCorrection, milliarcSeconds);
        }

        Assert.assertEquals(0.064, minCorrection, 0.001);
        Assert.assertEquals(0.613, maxCorrection, 0.001);

    }

    @Test
    public void testShift() throws OrekitException {
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF2005(false);
        final AbsoluteDate date0 = new AbsoluteDate(2007, 10, 20, TimeScalesFactory.getUTC());

        for (double t = 0; t < Constants.JULIAN_DAY; t += 3600) {
            final AbsoluteDate date = date0.shiftedBy(t);
            final Transform transform = gcrf.getTransformTo(itrf, date);
            for (double dt = -10; dt < 10; dt += 0.125) {
                final Transform shifted  = transform.shiftedBy(dt);
                final Transform computed = gcrf.getTransformTo(itrf, transform.getDate().shiftedBy(dt));
                final Transform error    = new Transform(computed.getDate(), computed, shifted.getInverse());
                Assert.assertEquals(0.0, error.getTranslation().getNorm(),  1.0e-10);
                Assert.assertEquals(0.0, error.getVelocity().getNorm(),     1.0e-10);
                Assert.assertEquals(0.0, error.getRotation().getAngle(),    1.0e-10);
                Assert.assertEquals(0.0, error.getRotationRate().getNorm(), 5.0e-15);
            }
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
