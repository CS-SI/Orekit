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

public class FixedTroposphericModelTest {

    private static double epsilon = 1e-6;

    private DiscreteTroposphericModel model;

    @Test
    public void testModel() {
        // check with (artificial) test values from tropospheric-delay.txt
        Assert.assertEquals(2.5d, model.pathDelay(FastMath.toRadians(90d), 0d, null, AbsoluteDate.J2000_EPOCH), epsilon);
        Assert.assertEquals(20.8d, model.pathDelay(FastMath.toRadians(0d), 0d, null, AbsoluteDate.J2000_EPOCH), epsilon);

        Assert.assertEquals(12.1d, model.pathDelay(FastMath.toRadians(0d), 5000d, null, AbsoluteDate.J2000_EPOCH), epsilon);
        Assert.assertEquals(2.5d, model.pathDelay(FastMath.toRadians(90d), 5000d, null, AbsoluteDate.J2000_EPOCH), epsilon);

        // interpolation between two elevation angles in the table
        final double delay = model.pathDelay(FastMath.toRadians(35d), 1200d, null, AbsoluteDate.J2000_EPOCH);
        Assert.assertTrue(Precision.compareTo(delay, 6.4d, epsilon) < 0);
        Assert.assertTrue(Precision.compareTo(delay, 3.2d, epsilon) > 0);

        // sanity checks
        Assert.assertEquals(12.1d, model.pathDelay(FastMath.toRadians(-20d), 5000d, null, AbsoluteDate.J2000_EPOCH), epsilon);
        Assert.assertEquals(2.5d, model.pathDelay(FastMath.toRadians(90d), 100000d, null, AbsoluteDate.J2000_EPOCH), epsilon);
    }

    @Test
    public void testFieldModel() {
        doTestFieldModel(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFieldModel(final Field<T> field) {
        final T zero = field.getZero();
        // check with (artificial) test values from tropospheric-delay.txt
        Assert.assertEquals(2.5d, model.pathDelay(zero.add(FastMath.toRadians(90d)), zero, null, FieldAbsoluteDate.getJ2000Epoch(field)).getReal(), epsilon);
        Assert.assertEquals(20.8d, model.pathDelay(zero.add(FastMath.toRadians(0d)), zero, null, FieldAbsoluteDate.getJ2000Epoch(field)).getReal(), epsilon);

        Assert.assertEquals(12.1d, model.pathDelay(zero.add(FastMath.toRadians(0d)), zero.add(5000d), null, FieldAbsoluteDate.getJ2000Epoch(field)).getReal(), epsilon);
        Assert.assertEquals(2.5d, model.pathDelay(zero.add(FastMath.toRadians(90d)), zero.add(5000d), null, FieldAbsoluteDate.getJ2000Epoch(field)).getReal(), epsilon);

        // interpolation between two elevation angles in the table
        final double delay = model.pathDelay(zero.add(FastMath.toRadians(35d)), zero.add(1200d), null, FieldAbsoluteDate.getJ2000Epoch(field)).getReal();
        Assert.assertTrue(Precision.compareTo(delay, 6.4d, epsilon) < 0);
        Assert.assertTrue(Precision.compareTo(delay, 3.2d, epsilon) > 0);

        // sanity checks
        Assert.assertEquals(12.1d, model.pathDelay(zero.add(FastMath.toRadians(-20d)), zero.add(5000d), null, FieldAbsoluteDate.getJ2000Epoch(field)).getReal(), epsilon);
        Assert.assertEquals(2.5d, model.pathDelay(zero.add(FastMath.toRadians(90d)), zero.add(100000d), null, FieldAbsoluteDate.getJ2000Epoch(field)).getReal(), epsilon);
    }

    @Test
    public void testZenithDelay() {
        // Zenith Delay
        final double[] zenithDelay = model.computeZenithDelay(0d, model.getParameters(), AbsoluteDate.J2000_EPOCH);
        Assert.assertEquals(2.5d, zenithDelay[0], epsilon);
        Assert.assertEquals(0.0d, zenithDelay[1], epsilon);

        // Compute delay using zenith delay and mapping factors
        // For Fixed Troposheric model, the delay is not split into hydrostatic and non-hydrostatic parts
        // In that respect, mapping function is equal to 1.0 for for both components and for any elevation angle
        final double[] mapping = model.mappingFactors(0d, 0d, model.getParameters(), AbsoluteDate.J2000_EPOCH);
        // Delay
        final double delay = zenithDelay[0] * mapping[0] + zenithDelay[1] * mapping[1];
        Assert.assertEquals(2.5d, delay, epsilon);
    }

    @Test
    public void testFieldZenithDelay() {
        doTestZenithDelay(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestZenithDelay(final Field<T> field) {
        // Zero
        final T zero = field.getZero();
        // Zenith Delay
        final T[] zenithDelay = model.computeZenithDelay(zero, model.getParameters(field), FieldAbsoluteDate.getJ2000Epoch(field));
        Assert.assertEquals(2.5d, zenithDelay[0].getReal(), epsilon);
        Assert.assertEquals(0.0d, zenithDelay[1].getReal(), epsilon);

        // Compute delay using zenith delay and mapping factors
        // For Fixed Troposheric model, the delay is not split into hydrostatic and non-hydrostatic parts
        // In that respect, mapping function is equal to 1.0 for for both components and for any elevation angle
        final T[] mapping = model.mappingFactors(zero, zero, model.getParameters(field), FieldAbsoluteDate.getJ2000Epoch(field));
        // Delay
        final T delay = zenithDelay[0].multiply(mapping[0]).add(zenithDelay[1].multiply(mapping[1]));
        Assert.assertEquals(2.5d, delay.getReal(), epsilon);
    }

    @Test
    public void testSymmetry() {
        for (int elevation = 0; elevation < 90; elevation += 10) {
            final double delay1 = model.pathDelay(FastMath.toRadians(elevation), 100, null, AbsoluteDate.J2000_EPOCH);
            final double delay2 = model.pathDelay(FastMath.toRadians(180 - elevation), 100, null, AbsoluteDate.J2000_EPOCH);

            Assert.assertEquals(delay1, delay2, epsilon);
        }
    }

    @Test
    public void testFieldSymmetry() {
        doTestFieldSymmetry(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFieldSymmetry(final Field<T> field) {
        final T zero = field.getZero();
        for (int elevation = 0; elevation < 90; elevation += 10) {
            final T delay1 = model.pathDelay(zero.add(FastMath.toRadians(elevation)),
                                             zero.add(100),
                                             null,
                                             FieldAbsoluteDate.getJ2000Epoch(field));
            final T delay2 = model.pathDelay(zero.add(FastMath.toRadians(180 - elevation)),
                                             zero.add(100),
                                             null,
                                             FieldAbsoluteDate.getJ2000Epoch(field));

            Assert.assertEquals(delay1.getReal(), delay2.getReal(), epsilon);
        }
    }

    @BeforeClass
    public static void setUpGlobal() {
        Utils.setDataRoot("atmosphere");
    }

    @Before
    public void setUp() throws Exception {
        model = FixedTroposphericDelay.getDefaultModel();
    }
}
