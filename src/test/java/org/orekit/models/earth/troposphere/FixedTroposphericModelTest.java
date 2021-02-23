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
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

public class FixedTroposphericModelTest {

    private static double epsilon = 1e-6;

    private DiscreteTroposphericModel model;

    @Test
    public void testModel() {
        // check with (artificial) test values from tropospheric-delay.txt
        Assert.assertEquals(2.5d, model.pathDelay(FastMath.toRadians(90d), new GeodeticPoint(0., 0., 0.), null, AbsoluteDate.J2000_EPOCH), epsilon);
        Assert.assertEquals(20.8d, model.pathDelay(FastMath.toRadians(0d), new GeodeticPoint(0., 0., 0.), null, AbsoluteDate.J2000_EPOCH), epsilon);

        Assert.assertEquals(12.1d, model.pathDelay(FastMath.toRadians(0d), new GeodeticPoint(0., 0., 5000.), null, AbsoluteDate.J2000_EPOCH), epsilon);
        Assert.assertEquals(2.5d, model.pathDelay(FastMath.toRadians(90d), new GeodeticPoint(0., 0., 5000.), null, AbsoluteDate.J2000_EPOCH), epsilon);

        // interpolation between two elevation angles in the table
        final double delay = model.pathDelay(FastMath.toRadians(35d), new GeodeticPoint(0., 0., 1200.), null, AbsoluteDate.J2000_EPOCH);
        Assert.assertTrue(Precision.compareTo(delay, 6.4d, epsilon) < 0);
        Assert.assertTrue(Precision.compareTo(delay, 3.2d, epsilon) > 0);

        // sanity checks
        Assert.assertEquals(12.1d, model.pathDelay(FastMath.toRadians(-20d), new GeodeticPoint(0., 0., 5000.), null, AbsoluteDate.J2000_EPOCH), epsilon);
        Assert.assertEquals(2.5d, model.pathDelay(FastMath.toRadians(90d), new GeodeticPoint(0., 0., 100000.), null, AbsoluteDate.J2000_EPOCH), epsilon);
    }

    @Test
    public void testFieldModel() {
        doTestFieldModel(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFieldModel(final Field<T> field) {
        final T zero = field.getZero();
        // check with (artificial) test values from tropospheric-delay.txt
        Assert.assertEquals(2.5d, model.pathDelay(zero.add(FastMath.toRadians(90d)), new FieldGeodeticPoint<T>(zero, zero, zero), null, FieldAbsoluteDate.getJ2000Epoch(field)).getReal(), epsilon);
        Assert.assertEquals(20.8d, model.pathDelay(zero.add(FastMath.toRadians(0d)), new FieldGeodeticPoint<T>(zero, zero, zero), null, FieldAbsoluteDate.getJ2000Epoch(field)).getReal(), epsilon);

        Assert.assertEquals(12.1d, model.pathDelay(zero.add(FastMath.toRadians(0d)), new FieldGeodeticPoint<T>(zero, zero, zero.add(5000.0)), null, FieldAbsoluteDate.getJ2000Epoch(field)).getReal(), epsilon);
        Assert.assertEquals(2.5d, model.pathDelay(zero.add(FastMath.toRadians(90d)), new FieldGeodeticPoint<T>(zero, zero, zero.add(5000.0)), null, FieldAbsoluteDate.getJ2000Epoch(field)).getReal(), epsilon);

        // interpolation between two elevation angles in the table
        final double delay = model.pathDelay(zero.add(FastMath.toRadians(35d)), new FieldGeodeticPoint<T>(zero, zero, zero.add(1200.0)), null, FieldAbsoluteDate.getJ2000Epoch(field)).getReal();
        Assert.assertTrue(Precision.compareTo(delay, 6.4d, epsilon) < 0);
        Assert.assertTrue(Precision.compareTo(delay, 3.2d, epsilon) > 0);

        // sanity checks
        Assert.assertEquals(12.1d, model.pathDelay(zero.add(FastMath.toRadians(-20d)), new FieldGeodeticPoint<T>(zero, zero, zero.add(5000.0)), null, FieldAbsoluteDate.getJ2000Epoch(field)).getReal(), epsilon);
        Assert.assertEquals(2.5d, model.pathDelay(zero.add(FastMath.toRadians(90d)), new FieldGeodeticPoint<T>(zero, zero, zero.add(100000.0)), null, FieldAbsoluteDate.getJ2000Epoch(field)).getReal(), epsilon);
    }

    @Test
    public void testSymmetry() {
        for (int elevation = 0; elevation < 90; elevation += 10) {
            final double delay1 = model.pathDelay(FastMath.toRadians(elevation), new GeodeticPoint(0., 0., 100.), null, AbsoluteDate.J2000_EPOCH);
            final double delay2 = model.pathDelay(FastMath.toRadians(180 - elevation), new GeodeticPoint(0., 0., 100.), null, AbsoluteDate.J2000_EPOCH);

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
                                             new FieldGeodeticPoint<T>(zero, zero, zero.add(100.)),
                                             null,
                                             FieldAbsoluteDate.getJ2000Epoch(field));
            final T delay2 = model.pathDelay(zero.add(FastMath.toRadians(180 - elevation)),
                                             new FieldGeodeticPoint<T>(zero, zero, zero.add(100.)),
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
