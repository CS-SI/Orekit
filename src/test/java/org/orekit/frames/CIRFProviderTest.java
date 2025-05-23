/* Copyright 2002-2025 CS GROUP
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
package org.orekit.frames;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.OrekitConfiguration;

import java.util.ArrayList;

public class CIRFProviderTest {

    @Test
    public void testRotationRate() {
        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);
        TransformProvider provider =
                new InterpolatingTransformProvider(new CIRFProvider(eopHistory),
                                                   CartesianDerivativesFilter.USE_PVA,
                                                   AngularDerivativesFilter.USE_R,
                                                   3, 1.0, 5, Constants.JULIAN_DAY, 100.0);
        AbsoluteDate tMin = new AbsoluteDate(2009, 4, 7, 2, 56, 33.816, TimeScalesFactory.getUTC());
        double minRate = provider.getTransform(tMin).getRotationRate().getNorm();
        Assertions.assertEquals(1.1e-15, minRate, 1.0e-16);
        AbsoluteDate tMax = new AbsoluteDate(2043, 12, 16, 10, 47, 20, TimeScalesFactory.getUTC());
        double maxRate = provider.getTransform(tMax).getRotationRate().getNorm();
        Assertions.assertEquals(8.6e-12, maxRate, 1.0e-13);
    }

    @Test
    public void testShiftingAccuracyWithEOP() {

        // max shift error observed on a 2 months period with 60 seconds step
        // the shifting step after the interpolation step induces that mainly the
        // step size has an influence on the error. The number of points used in
        // the lower level interpolation does affect the results but less than
        // step size. Plotting the error curves show almost superposition of all
        // curves with the same time step. The error curves exhibit smooth periodic
        // pattern with a main period corresponding to the Moon period plus peaks
        // corresponding to Runge phenomenon when EOP data changes at midnight.
        //
        // number of sample points    time between sample points    max error
        //         6                         86400s / 12 = 2h00    9.56e-12 rad
        //         6                         86400s / 18 = 1h20    6.20e-12 rad
        //         6                         86400s / 24 = 1h00    4.60e-12 rad
        //         6                         86400s / 48 = 0h30    2.25e-12 rad
        //         8                         86400s / 12 = 2h00    8.21e-12 rad
        //         8                         86400s / 18 = 1h20    5.32e-12 rad
        //         8                         86400s / 24 = 1h00    3.94e-12 rad
        //         8                         86400s / 48 = 0h30    1.93e-12 rad
        //        12                         86400s / 12 = 2h00    7.09e-12 rad
        //        12                         86400s / 18 = 1h20    4.61e-12 rad
        //        12                         86400s / 24 = 1h00    3.42e-12 rad
        //        12                         86400s / 48 = 0h30    1.68e-12 rad
        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, false);
        TransformProvider nonShitfing = new CIRFProvider(eopHistory);
        final TransformProvider shifting =
                new ShiftingTransformProvider(nonShitfing,
                                              CartesianDerivativesFilter.USE_PVA,
                                              AngularDerivativesFilter.USE_R,
                                              6, Constants.JULIAN_DAY / 24,
                                              OrekitConfiguration.getCacheSlotsNumber(),
                                              Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);

        // the following time range is located around the maximal observed error
        AbsoluteDate start = new AbsoluteDate(2002, 9, 12, TimeScalesFactory.getTAI());
        AbsoluteDate end   = new AbsoluteDate(2002, 9, 14, TimeScalesFactory.getTAI());
        double maxError = 0.0;
        for (AbsoluteDate date = start; date.compareTo(end) < 0; date = date.shiftedBy(60)) {
            final Transform transform =
                    new Transform(date,
                                  shifting.getTransform(date),
                                  nonShitfing.getTransform(date).getInverse());
            final double error = transform.getRotation().getAngle();
            maxError = FastMath.max(maxError, error);
        }
        Assertions.assertTrue(maxError < 4.6e-12);

    }

    @Test
    public void testShiftingAccuracyWithoutEOP() {

        // max shift error observed on a 2 months period with 60 seconds step
        // the shifting step after the interpolation step induces that only the
        // step size has an influence on the error. The number of points used in
        // the lower level interpolation does not affect the results. Plotting
        // the error curves show exact superposition of all curves with the same
        // time step. The error curves exhibit smooth periodic pattern with a
        // main period corresponding to the Moon period
        //
        // number of sample points    time between sample points    max error
        //         6                         86400s / 12 = 2h00    9.99e-13 rad
        //         6                         86400s / 18 = 1h20    2.96e-13 rad
        //         6                         86400s / 24 = 1h00    1.25e-13 rad
        //         6                         86400s / 48 = 0h30    1.56e-14 rad
        //         8                         86400s / 12 = 2h00    9.99e-13 rad
        //         8                         86400s / 18 = 1h20    2.96e-13 rad
        //         8                         86400s / 24 = 1h00    1.25e-13 rad
        //         8                         86400s / 48 = 0h30    1.56e-14 rad
        //        12                         86400s / 12 = 2h00    9.99e-13 rad
        //        12                         86400s / 18 = 1h20    2.96e-13 rad
        //        12                         86400s / 24 = 1h00    1.25e-13 rad
        //        12                         86400s / 48 = 0h30    1.56e-14 rad
        EOPHistory eopHistory = new EOPHistory(IERSConventions.IERS_2010, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                               new ArrayList<EOPEntry>(), true);
        TransformProvider nonShifting = new CIRFProvider(eopHistory);
        final TransformProvider shifting =
                new ShiftingTransformProvider(nonShifting,
                                              CartesianDerivativesFilter.USE_PVA,
                                              AngularDerivativesFilter.USE_R,
                                              6, Constants.JULIAN_DAY / 24,
                                              OrekitConfiguration.getCacheSlotsNumber(),
                                              Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);

        // the following time range is located around the maximal observed error
        AbsoluteDate start = new AbsoluteDate(2002, 9, 7, TimeScalesFactory.getTAI());
        AbsoluteDate end   = new AbsoluteDate(2002, 9, 8, TimeScalesFactory.getTAI());
        double maxError = 0.0;
        for (AbsoluteDate date = start; date.compareTo(end) < 0; date = date.shiftedBy(60)) {
            final Transform transform =
                    new Transform(date,
                                  shifting.getTransform(date),
                                  nonShifting.getTransform(date).getInverse());
            final double error = transform.getRotation().getAngle();
            maxError = FastMath.max(maxError, error);
        }
        Assertions.assertTrue(maxError < 1.3e-13);

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
