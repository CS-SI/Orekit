/* Copyright 2011-2012 Space Applications Services
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
package org.orekit.models.earth.troposphere;


import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

public class MariniMurrayModelTest {

    private static double epsilon = 1e-6;

    private DiscreteTroposphericModel model;

    @BeforeClass
    public static void setUpGlobal() {
        Utils.setDataRoot("atmosphere");
    }

    @Before
    public void setUp() throws Exception {
        // ruby laser with wavelength 694.3 nm
        model = MariniMurrayModel.getStandardModel(FastMath.toRadians(45.0), 694.3);
    }

    @Test
    public void testDelay() {
        final double elevation = 10d;
        final double height = 100d;

        final double path = model.pathDelay(FastMath.toRadians(elevation), height, null, AbsoluteDate.J2000_EPOCH);

        Assert.assertTrue(Precision.compareTo(path, 20d, epsilon) < 0);
        Assert.assertTrue(Precision.compareTo(path, 0d, epsilon) > 0);
    }

    @Test
    public void testFieldDelay() {
        doTestFieldDelay(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFieldDelay(final Field<T> field) {
        final T zero = field.getZero();
        final T elevation = zero.add(FastMath.toRadians(10d));
        final T height = zero.add(100d);

        final T path = model.pathDelay(elevation, height, null, FieldAbsoluteDate.getJ2000Epoch(field));

        Assert.assertTrue(Precision.compareTo(path.getReal(), 20d, epsilon) < 0);
        Assert.assertTrue(Precision.compareTo(path.getReal(), 0d, epsilon) > 0);
    }

    @Test
    public void testFixedHeight() {
        double lastDelay = Double.MAX_VALUE;
        // delay shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final double delay = model.pathDelay(FastMath.toRadians(elev), 350, null, AbsoluteDate.J2000_EPOCH);
            Assert.assertTrue(Precision.compareTo(delay, lastDelay, epsilon) < 0);
            lastDelay = delay;
        }
    }

    @Test
    public void testFieldFixedHeight() {
        doTestFieldFixedHeight(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFieldFixedHeight(final Field<T> field) {
        final T zero = field.getZero();
        T lastDelay  = zero.add(Double.MAX_VALUE);
        // delay shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final T delay = model.pathDelay(zero.add(FastMath.toRadians(elev)), zero.add(350), null, FieldAbsoluteDate.getJ2000Epoch(field));
            Assert.assertTrue(Precision.compareTo(delay.getReal(), lastDelay.getReal(), epsilon) < 0);
            lastDelay = delay;
        }
    }

    @Test
    public void compareExpectedValues() {

        double height = 0;
        double elevation = 10;
        double expectedValue = 13.26069;
        double actualValue = model.pathDelay(FastMath.toRadians(elevation), height, null, AbsoluteDate.J2000_EPOCH);

        Assert.assertEquals(expectedValue, actualValue, 1.0e-5);
    }

    @Test
    public void compareFieldExpectedValue() {
        doCompareFieldExpectedValues(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doCompareFieldExpectedValues(final Field<T> field) {

        T zero = field.getZero();
        T height = zero;
        T elevation = zero.add(FastMath.toRadians(10));
        double expectedValue = 13.26069;
        T actualValue = model.pathDelay(elevation, height, null, FieldAbsoluteDate.getJ2000Epoch(field));

        Assert.assertEquals(expectedValue, actualValue.getReal(), 1.0e-5);
    }

    @Test
    public void testSameDelay() {

        // Input parameters
        double height = 0;
        double elevation = 90;

        // Compute delay using pathDelay method
        final double delay1 = model.pathDelay(FastMath.toRadians(elevation), height, model.getParameters(), AbsoluteDate.J2000_EPOCH);

        // Compute delay using zenith delay and mapping factors
        // For Marini Murray model, the delay is not split into hydrostatic and non-hydrostatic parts
        // In that respect, mapping function is equal to 1.0 for for both components and for any elevation angle
        final double[] zenith  = model.computeZenithDelay(height, model.getParameters(), AbsoluteDate.J2000_EPOCH);
        final double[] mapping = model.mappingFactors(elevation, height, model.getParameters(), AbsoluteDate.J2000_EPOCH);
        // Delay
        final double delay2 = zenith[0] * mapping[0] + zenith[1] * mapping[1];

        // Verify
        Assert.assertEquals(delay1, delay2, 1.0e-5);

    }

    @Test
    public void testFieldSameDelay() {
        doTestFieldSameDelay(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFieldSameDelay(final Field<T> field) {

        // Zero
        final T zero = field.getZero();

        // Input parameters
        T height = zero;
        T elevation = zero.add(FastMath.toRadians(90));

        // Compute delay using pathDelay method
        final T delay1 = model.pathDelay(elevation, height, model.getParameters(field), FieldAbsoluteDate.getJ2000Epoch(field));

        // Compute delay using zenith delay and mapping factors
        // For Marini Murray model, the delay is not split into hydrostatic and non-hydrostatic parts
        // In that respect, mapping function is equal to 1.0 for for both components and for any elevation angle
        final T[] zenith  = model.computeZenithDelay(height, model.getParameters(field), FieldAbsoluteDate.getJ2000Epoch(field));
        final T[] mapping = model.mappingFactors(elevation, height, model.getParameters(field), FieldAbsoluteDate.getJ2000Epoch(field));
        // Delay
        final T delay2 = zenith[0].multiply(mapping[0]).add(zenith[1].multiply(mapping[1]));

        // Verify
        Assert.assertEquals(delay1.getReal(), delay2.getReal(), 1.0e-5);

    }

}
