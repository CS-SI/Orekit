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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

public class MariniMurrayModelTest {

    private static double epsilon = 1e-6;

    private DiscreteTroposphericModel model;

    private double latitude;

    private double longitude;

    @BeforeAll
    public static void setUpGlobal() {
        Utils.setDataRoot("atmosphere");
    }

    @BeforeEach
    public void setUp() throws Exception {
        // ruby laser with wavelength 694.3 nm
        model = MariniMurrayModel.getStandardModel(694.3);
        latitude = FastMath.toRadians(45.0);
        longitude = FastMath.toRadians(45.0);
    }

    @Test
    public void testDelay() {
        final double elevation = 10d;
        final double height = 100d;

        final double path = model.pathDelay(FastMath.toRadians(elevation), new GeodeticPoint(latitude, longitude, height), null, AbsoluteDate.J2000_EPOCH);

        Assertions.assertTrue(Precision.compareTo(path, 20d, epsilon) < 0);
        Assertions.assertTrue(Precision.compareTo(path, 0d, epsilon) > 0);
    }

    @Test
    public void testFieldDelay() {
        doTestFieldDelay(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldDelay(final Field<T> field) {
        final T zero = field.getZero();
        final T elevation = zero.add(FastMath.toRadians(10d));
        final T height = zero.add(100d);

        final T path = model.pathDelay(elevation, new FieldGeodeticPoint<>(zero.add(latitude), zero.add(longitude), height), null, FieldAbsoluteDate.getJ2000Epoch(field));

        Assertions.assertTrue(Precision.compareTo(path.getReal(), 20d, epsilon) < 0);
        Assertions.assertTrue(Precision.compareTo(path.getReal(), 0d, epsilon) > 0);
    }

    @Test
    public void testFixedHeight() {
        double lastDelay = Double.MAX_VALUE;
        // delay shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final double delay = model.pathDelay(FastMath.toRadians(elev), new GeodeticPoint(latitude, longitude, 350.0), null, AbsoluteDate.J2000_EPOCH);
            Assertions.assertTrue(Precision.compareTo(delay, lastDelay, epsilon) < 0);
            lastDelay = delay;
        }
    }

    @Test
    public void testFieldFixedHeight() {
        doTestFieldFixedHeight(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldFixedHeight(final Field<T> field) {
        final T zero = field.getZero();
        T lastDelay  = zero.add(Double.MAX_VALUE);
        // delay shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final T delay = model.pathDelay(zero.add(FastMath.toRadians(elev)), new FieldGeodeticPoint<>(zero.add(latitude), zero.add(longitude), zero.add(350.0)), null, FieldAbsoluteDate.getJ2000Epoch(field));
            Assertions.assertTrue(Precision.compareTo(delay.getReal(), lastDelay.getReal(), epsilon) < 0);
            lastDelay = delay;
        }
    }

    @Test
    public void compareExpectedValues() {

        double height = 0;
        double elevation = 10;
        double expectedValue = 13.26069;
        double actualValue = model.pathDelay(FastMath.toRadians(elevation), new GeodeticPoint(latitude, longitude, height), null, AbsoluteDate.J2000_EPOCH);

        Assertions.assertEquals(expectedValue, actualValue, 1.0e-5);
    }

    @Test
    public void compareFieldExpectedValue() {
        doCompareFieldExpectedValues(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doCompareFieldExpectedValues(final Field<T> field) {

        T zero = field.getZero();
        T height = zero;
        T elevation = zero.add(FastMath.toRadians(10));
        double expectedValue = 13.26069;
        T actualValue = model.pathDelay(elevation, new FieldGeodeticPoint<>(zero.add(latitude), zero.add(longitude), height), null, FieldAbsoluteDate.getJ2000Epoch(field));

        Assertions.assertEquals(expectedValue, actualValue.getReal(), 1.0e-5);
    }

}
