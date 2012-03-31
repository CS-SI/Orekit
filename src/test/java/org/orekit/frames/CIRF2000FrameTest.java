/* Copyright 2002-2012 CS Systèmes d'Information
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


import java.io.FileNotFoundException;

import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class CIRF2000FrameTest {

    @Test
    public void testInterpolationAccuracy() throws OrekitException, FileNotFoundException {

        CIRF2000Frame interpolatingFrame = new CIRF2000Frame(Predefined.CIRF_2000);
        CIRF2000Frame nonInterpolatingFrame = new NonInterpolatingCIRF2000Frame(Predefined.CIRF_2000);

        // the following time range is located around the maximal observed error
        AbsoluteDate start = new AbsoluteDate(2002, 10,  3, TimeScalesFactory.getTAI());
        AbsoluteDate end   = new AbsoluteDate(2002, 10,  7, TimeScalesFactory.getTAI());
        double maxError = 0.0;
        for (AbsoluteDate date = start; date.compareTo(end) < 0; date = date.shiftedBy(900)) {
            final Transform transform =
                interpolatingFrame.getTransformTo(nonInterpolatingFrame, date);
            final double error = transform.getRotation().getAngle() * 648000 / FastMath.PI;
            maxError = FastMath.max(maxError, error);
        }

        Assert.assertTrue(maxError < 1.3e-10);

    }

    private class NonInterpolatingCIRF2000Frame extends CIRF2000Frame {
        private static final long serialVersionUID = -8187118174897725897L;
        public NonInterpolatingCIRF2000Frame(final Predefined factoryKey)
            throws OrekitException {
            super(factoryKey);
        }
        protected void setInterpolatedPoleCoordinates(final double t) {
            computePoleCoordinates(t);
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
