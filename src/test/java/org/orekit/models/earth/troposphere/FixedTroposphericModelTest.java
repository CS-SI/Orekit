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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.TrackingCoordinates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedTroposphericModelTest {

    private static double epsilon = 1e-6;

    private TroposphericModel model;

    @Test
    void testModel() {
        // check with (artificial) test values from tropospheric-delay.txt
        assertEquals(2.5d,
                                model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(90d), 0.0),
                                                new GeodeticPoint(0., 0., 0.),
                                                TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                null, AbsoluteDate.J2000_EPOCH).getDelay(),
                                epsilon);
        assertEquals(20.8d,
                                model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(0d), 0.0),
                                                new GeodeticPoint(0., 0., 0.),
                                                TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                null, AbsoluteDate.J2000_EPOCH).getDelay(),
                                epsilon);

        assertEquals(12.1d,
                                model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(0d), 0.0),
                                                new GeodeticPoint(0., 0., 5000.),
                                                TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                null, AbsoluteDate.J2000_EPOCH).getDelay(),
                                epsilon);
        assertEquals(2.5d,
                                model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(90d), 0.0),
                                                new GeodeticPoint(0., 0., 5000.),
                                                TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                null, AbsoluteDate.J2000_EPOCH).getDelay(),
                                epsilon);

        // interpolation between two elevation angles in the table
        final double delay = model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(35d), 0.0),
                                             new GeodeticPoint(0., 0., 1200.),
                                             TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                             null, AbsoluteDate.J2000_EPOCH).getDelay();
        assertTrue(Precision.compareTo(delay, 6.4d, epsilon) < 0);
        assertTrue(Precision.compareTo(delay, 3.2d, epsilon) > 0);

        // sanity checks
        assertEquals(12.1d,
                                model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(-20d), 0.0),
                                                new GeodeticPoint(0., 0., 5000.),
                                                TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                null, AbsoluteDate.J2000_EPOCH).getDelay(),
                                epsilon);
        assertEquals(2.5d,
                                model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(90d),0.0),
                                                new GeodeticPoint(0., 0., 100000.),
                                                TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                null, AbsoluteDate.J2000_EPOCH).getDelay(),
                                epsilon);
    }

    @Test
    void testFieldModel() {
        doTestFieldModel(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldModel(final Field<T> field) {
        final T zero = field.getZero();
        // check with (artificial) test values from tropospheric-delay.txt
        assertEquals(2.5d,
                                model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(90d)), zero),
                                                new FieldGeodeticPoint<T>(zero, zero, zero),
                                                new FieldPressureTemperatureHumidity<T>(field, TroposphericModelUtils.STANDARD_ATMOSPHERE),
                                                null, FieldAbsoluteDate.getJ2000Epoch(field)).getDelay().getReal(),
                                epsilon);
        assertEquals(20.8d,
                                model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(0d)), zero),
                                                new FieldGeodeticPoint<T>(zero, zero, zero),
                                                new FieldPressureTemperatureHumidity<T>(field, TroposphericModelUtils.STANDARD_ATMOSPHERE),
                                                null, FieldAbsoluteDate.getJ2000Epoch(field)).getDelay().getReal(),
                                epsilon);

        assertEquals(12.1d,
                                model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(0d)), zero),
                                                new FieldGeodeticPoint<T>(zero, zero, zero.add(5000.0)),
                                                new FieldPressureTemperatureHumidity<T>(field, TroposphericModelUtils.STANDARD_ATMOSPHERE),
                                                null, FieldAbsoluteDate.getJ2000Epoch(field)).getDelay().getReal(),
                                epsilon);
        assertEquals(2.5d,
                                model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(90d)), zero),
                                                new FieldGeodeticPoint<T>(zero, zero, zero.add(5000.0)),
                                                new FieldPressureTemperatureHumidity<T>(field, TroposphericModelUtils.STANDARD_ATMOSPHERE),
                                                null, FieldAbsoluteDate.getJ2000Epoch(field)).getDelay().getReal(),
                                epsilon);

        // interpolation between two elevation angles in the table
        final double delay = model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(35d)), zero),
                                             new FieldGeodeticPoint<T>(zero, zero, zero.add(1200.0)),
                                             new FieldPressureTemperatureHumidity<T>(field, TroposphericModelUtils.STANDARD_ATMOSPHERE),
                                             null, FieldAbsoluteDate.getJ2000Epoch(field)).getDelay().getReal();
        assertTrue(Precision.compareTo(delay, 6.4d, epsilon) < 0);
        assertTrue(Precision.compareTo(delay, 3.2d, epsilon) > 0);

        // sanity checks
        assertEquals(12.1d,
                                model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(-20d)), zero),
                                                new FieldGeodeticPoint<T>(zero, zero, zero.add(5000.0)),
                                                new FieldPressureTemperatureHumidity<T>(field, TroposphericModelUtils.STANDARD_ATMOSPHERE),
                                                null, FieldAbsoluteDate.getJ2000Epoch(field)).getDelay().getReal(),
                                epsilon);
        assertEquals(2.5d,
                                model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(90d)), zero),
                                                new FieldGeodeticPoint<T>(zero, zero, zero.add(100000.0)),
                                                new FieldPressureTemperatureHumidity<T>(field, TroposphericModelUtils.STANDARD_ATMOSPHERE),
                                                null, FieldAbsoluteDate.getJ2000Epoch(field)).getDelay().getReal(),
                                epsilon);
    }

    @Test
    void testSymmetry() {
        for (int elevation = 0; elevation < 90; elevation += 10) {
            final double delay1 = model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(elevation), 0.0),
                                                  new GeodeticPoint(0., 0., 100.),
                                                  TroposphericModelUtils.STANDARD_ATMOSPHERE, null, AbsoluteDate.J2000_EPOCH).getDelay();
            final double delay2 = model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(180 - elevation), 0.0),
                                                  new GeodeticPoint(0., 0., 100.),
                                                  TroposphericModelUtils.STANDARD_ATMOSPHERE, null, AbsoluteDate.J2000_EPOCH).getDelay();

            assertEquals(delay1, delay2, epsilon);
        }
    }

    @Test
    void testFieldSymmetry() {
        doTestFieldSymmetry(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldSymmetry(final Field<T> field) {
        final T zero = field.getZero();
        for (int elevation = 0; elevation < 90; elevation += 10) {
            final T delay1 = model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(elevation)), zero),
                                             new FieldGeodeticPoint<T>(zero, zero, zero.add(100.)),
                                             new FieldPressureTemperatureHumidity<T>(field, TroposphericModelUtils.STANDARD_ATMOSPHERE),
                                             null,
                                             FieldAbsoluteDate.getJ2000Epoch(field)).getDelay();
            final T delay2 = model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(180 - elevation)), zero),
                                             new FieldGeodeticPoint<T>(zero, zero, zero.add(100.)),
                                             new FieldPressureTemperatureHumidity<T>(field, TroposphericModelUtils.STANDARD_ATMOSPHERE),
                                                             null,
                                             FieldAbsoluteDate.getJ2000Epoch(field)).getDelay();

            assertEquals(delay1.getReal(), delay2.getReal(), epsilon);
        }
    }

    @BeforeAll
    static void setUpGlobal() {
        Utils.setDataRoot("atmosphere");
    }

    @BeforeEach
    void setUp() {
        model = FixedTroposphericDelay.getDefaultModel();
    }
}
