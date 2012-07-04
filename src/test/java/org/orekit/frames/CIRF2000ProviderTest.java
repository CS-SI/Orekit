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


import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.OrekitConfiguration;

public class CIRF2000ProviderTest {

    @Test
    public void testInterpolationAccuracy() throws OrekitException {

        // max interpolation error observed on a 2 months period with 60 seconds step
        // all values between 3e-15 and 8e-15 are really equivalent: it is mostly numerical noise
        //
        // number of sample points    time between sample points    max error
        //        6                          86400s / 2 = 12h        2259.1e-15 rad
        //        6                          86400s / 4 =  6h          35.6e-15 rad
        //        6                          86400s / 6 =  4h           5.4e-15 rad
        //        6                          86400s / 8 =  3h           3.6e-15 rad
        //        8                          86400s / 2 = 12h         103.8e-15 rad
        //        8                          86400s / 4 =  6h           4.8e-15 rad
        //        8                          86400s / 6 =  4h           4.0e-15 rad
        //        8                          86400s / 8 =  3h           4.2e-15 rad
        //       10                          86400s / 2 = 12h           8.3e-15 rad
        //       10                          86400s / 4 =  6h           5.3e-15 rad
        //       10                          86400s / 6 =  4h           5.2e-15 rad
        //       10                          86400s / 8 =  3h           6.1e-15 rad
        //       12                          86400s / 2 = 12h           6.3e-15 rad
        //       12                          86400s / 4 =  6h           7.8e-15 rad
        //       12                          86400s / 6 =  4h           7.2e-15 rad
        //       12                          86400s / 8 =  3h           6.9e-15 rad
        //
        // the two best settings are 6 points every 3 hours and 8 points every 4 hours
        TransformProvider nonInterpolating = new CIRF2000Provider();
        final TransformProvider interpolating =
                new InterpolatingTransformProvider(nonInterpolating, true, false,
                                                   AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                                                   8, Constants.JULIAN_DAY / 6,
                                                   OrekitConfiguration.getDefaultMaxSlotsNumber(),
                                                   Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);

        // the following time range is located around the maximal observed error
        AbsoluteDate start = new AbsoluteDate(2002, 10,  3, TimeScalesFactory.getTAI());
        AbsoluteDate end   = new AbsoluteDate(2002, 10,  7, TimeScalesFactory.getTAI());
        double maxError = 0.0;
        for (AbsoluteDate date = start; date.compareTo(end) < 0; date = date.shiftedBy(300)) {
            final Transform transform =
                    new Transform(date,
                                  interpolating.getTransform(date),
                                  nonInterpolating.getTransform(date).getInverse());
            final double error = transform.getRotation().getAngle();
            maxError = FastMath.max(maxError, error);
        }
        Assert.assertTrue(maxError < 4.0e-15);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
