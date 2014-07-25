/* Copyright 2002-2014 CS Systèmes d'Information
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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.OrekitConfiguration;

public class CIRFProviderTest {

    @Test
    public void testRotationRate() throws OrekitException {
        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);
        TransformProvider provider =
                new InterpolatingTransformProvider(new CIRFProvider(eopHistory),
                                                   CartesianDerivativesFilter.USE_PVA,
                                                   AngularDerivativesFilter.USE_R,
                                                   AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                                                   3, 1.0, 5, Constants.JULIAN_DAY, 100.0);
        AbsoluteDate tMin = new AbsoluteDate(2009, 4, 7, 2, 56, 33.816, TimeScalesFactory.getUTC());
        double minRate = provider.getTransform(tMin).getRotationRate().getNorm();
        Assert.assertEquals(1.1e-15, minRate, 1.0e-16);
        AbsoluteDate tMax = new AbsoluteDate(2043, 12, 16, 10, 47, 20, TimeScalesFactory.getUTC());
        double maxRate = provider.getTransform(tMax).getRotationRate().getNorm();
        Assert.assertEquals(8.6e-12, maxRate, 1.0e-13);
    }

    @Test
    public void testInterpolationAccuracyWithEOP() throws OrekitException {

        // max interpolation error observed on a 2 months period with 60 seconds step
        // all values between 1e-12 and 5e-12. It mainy shows Runge phenomenon at EOP
        // files sampling rate, and the error drops to about 5e-17 when EOP are ignored.
        //
        // number of sample points    time between sample points    max error
        //         6                         86400s / 12 = 2h00    4.55e-12 rad
        //         6                         86400s / 18 = 1h20    3.02e-12 rad
        //         6                         86400s / 24 = 1h00    2.26e-12 rad
        //         6                         86400s / 48 = 0h30    1.08e-12 rad
        //         8                         86400s / 12 = 2h00    4.87e-12 rad
        //         8                         86400s / 18 = 1h20    3.23e-12 rad
        //         8                         86400s / 24 = 1h00    2.42e-12 rad
        //         8                         86400s / 48 = 0h30    1.16e-12 rad
        //        12                         86400s / 12 = 2h00    5.15e-12 rad
        //        12                         86400s / 18 = 1h20    3.41e-12 rad
        //        12                         86400s / 24 = 1h00    2.56e-12 rad
        //        12                         86400s / 48 = 0h30    1.22e-12 rad
        // as Runge phenomenon is the dimensioning part, the best settings are a small number
        // of points and a small interval. Four points separated by one hour each
        // implies an interpolation error of 1.89e-12 at peak of Runge oscillations,
        // and about 2e-15 away from the singularity points
        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, false);
        TransformProvider nonInterpolating = new CIRFProvider(eopHistory);
        final TransformProvider interpolating =
                new InterpolatingTransformProvider(nonInterpolating,
                                                   CartesianDerivativesFilter.USE_PVA,
                                                   AngularDerivativesFilter.USE_R,
                                                   AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                                                   6, Constants.JULIAN_DAY / 24,
                                                   OrekitConfiguration.getCacheSlotsNumber(),
                                                   Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);

        // the following time range is located around the maximal observed error
        AbsoluteDate start = new AbsoluteDate(2002, 9, 12, TimeScalesFactory.getTAI());
        AbsoluteDate end   = new AbsoluteDate(2002, 9, 14, TimeScalesFactory.getTAI());
        double maxError = 0.0;
        for (AbsoluteDate date = start; date.compareTo(end) < 0; date = date.shiftedBy(300)) {
            final Transform transform =
                    new Transform(date,
                                  interpolating.getTransform(date),
                                  nonInterpolating.getTransform(date).getInverse());
            final double error = transform.getRotation().getAngle();
            maxError = FastMath.max(maxError, error);
        }
        Assert.assertTrue(maxError < 3.0e-12);

    }

    @Test
    public void testInterpolationAccuracyWithoutEOP() throws OrekitException {

        // max interpolation error observed on a 2 months period with 60 seconds step
        // all values between 4e-17 and 6e-17. It it essentially numerical noise, with a
        // very slight Runge phenomenon for 6 points separated by 2 hours
        //
        // number of sample points    time between sample points    max error
        //         6                         86400s / 12 = 2h00    6.04e-17 rad
        //         6                         86400s / 18 = 1h20    4.69e-17 rad
        //         6                         86400s / 24 = 1h00    5.09e-17 rad
        //         6                         86400s / 48 = 0h30    4.79e-17 rad
        //         8                         86400s / 12 = 2h00    5.02e-17 rad
        //         8                         86400s / 18 = 1h20    4.74e-17 rad
        //         8                         86400s / 24 = 1h00    5.10e-17 rad
        //         8                         86400s / 48 = 0h30    4.83e-17 rad
        //        12                         86400s / 12 = 2h00    5.06e-17 rad
        //        12                         86400s / 18 = 1h20    4.79e-17 rad
        //        12                         86400s / 24 = 1h00    5.09e-17 rad
        //        12                         86400s / 48 = 0h30    4.86e-17 rad
        EOPHistory eopHistory = new EOPHistory(IERSConventions.IERS_2010, new ArrayList<EOPEntry>(), true);
        TransformProvider nonInterpolating = new CIRFProvider(eopHistory);
        final TransformProvider interpolating =
                new InterpolatingTransformProvider(nonInterpolating,
                                                   CartesianDerivativesFilter.USE_PVA,
                                                   AngularDerivativesFilter.USE_R,
                                                   AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                                                   6, Constants.JULIAN_DAY / 24,
                                                   OrekitConfiguration.getCacheSlotsNumber(),
                                                   Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);

        // the following time range is located around the maximal observed error
        AbsoluteDate start = new AbsoluteDate(2002, 9, 12, TimeScalesFactory.getTAI());
        AbsoluteDate end   = new AbsoluteDate(2002, 9, 14, TimeScalesFactory.getTAI());
        double maxError = 0.0;
        for (AbsoluteDate date = start; date.compareTo(end) < 0; date = date.shiftedBy(300)) {
            final Transform transform =
                    new Transform(date,
                                  interpolating.getTransform(date),
                                  nonInterpolating.getTransform(date).getInverse());
            final double error = transform.getRotation().getAngle();
            maxError = FastMath.max(maxError, error);
        }
        Assert.assertTrue(maxError < 6.0e-17);

    }

    @Test
    public void testSerialization() throws OrekitException, IOException, ClassNotFoundException {
        CIRFProvider provider = new CIRFProvider(FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true));
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(provider);

        Assert.assertTrue(bos.size() > 280000);
        Assert.assertTrue(bos.size() < 285000);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        CIRFProvider deserialized  = (CIRFProvider) ois.readObject();
        for (int i = 0; i < FastMath.min(100, provider.getEOPHistory().getEntries().size()); ++i) {
            AbsoluteDate date = provider.getEOPHistory().getEntries().get(i).getDate();
            Transform expectedIdentity = new Transform(date,
                                                       provider.getTransform(date).getInverse(),
                                                       deserialized.getTransform(date));
            Assert.assertEquals(0.0, expectedIdentity.getTranslation().getNorm(), 1.0e-15);
            Assert.assertEquals(0.0, expectedIdentity.getRotation().getAngle(),   1.0e-15);
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
