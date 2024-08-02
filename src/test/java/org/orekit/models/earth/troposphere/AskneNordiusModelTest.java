/* Copyright 2002-2024 Thales Alenia Space
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
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.HeightDependentPressureTemperatureHumidityConverter;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.water.CIPM2007;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.TrackingCoordinates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class AskneNordiusModelTest {

    @Test
    void testFixedElevation() {
        Utils.setDataRoot("atmosphere");
        // first line of GPT 2w grid
        PressureTemperatureHumidity pth = new PressureTemperatureHumidity(0.0,
                                                                          101421, 259.2,
                                                                          new CIPM2007().waterVaporPressure(101421, 259.2, 1.64),
                                                                          255.1, 1.665);
        AskneNordiusModel model = new AskneNordiusModel(new DummyMappingFunction());

        HeightDependentPressureTemperatureHumidityConverter converter =
                        new HeightDependentPressureTemperatureHumidityConverter(new CIPM2007());
        double lastDelay = Double.MAX_VALUE;
        // delay shall decline with increasing height of the station
        for (double height = 0; height < 5000; height += 100) {
            final double delay = model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(5), 0.0),
                                                 new GeodeticPoint(0.0, 0.0, height),
                                                 converter.convert(pth, height),
                                                 null, AbsoluteDate.J2000_EPOCH).getZw();
            assertTrue(Precision.compareTo(delay, lastDelay, 1.0e-6) < 0);
            lastDelay = delay;
        }
    }

    @Test
    void testFieldFixedElevation() {
        doTestFieldFixedElevation(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldFixedElevation(final Field<T> field) {
        final T zero = field.getZero();
        Utils.setDataRoot("atmosphere");
        // first line of GPT 2w grid
        PressureTemperatureHumidity pth = new PressureTemperatureHumidity(0.0,
                                                                          101421, 259.2,
                                                                          new CIPM2007().waterVaporPressure(101421, 259.2, 1.64),
                                                                          255.1, 1.665);
        AskneNordiusModel model = new AskneNordiusModel(new DummyMappingFunction());
        HeightDependentPressureTemperatureHumidityConverter converter =
                        new HeightDependentPressureTemperatureHumidityConverter(new CIPM2007());
        T lastDelay = zero.newInstance(Double.MAX_VALUE);
        // delay shall decline with increasing height of the station
        for (double height = 0; height < 5000; height += 100) {
            final T delay = model.pathDelay(new FieldTrackingCoordinates<>(zero,
                                                                           zero.newInstance(FastMath.toRadians(5)),
                                                                           zero),
                                            new FieldGeodeticPoint<>(zero, zero, zero.newInstance(height)),
                                            converter.convert(new FieldPressureTemperatureHumidity<>(field, pth),
                                                              zero.newInstance(height)),
                                            null, FieldAbsoluteDate.getJ2000Epoch(field)).getZw();
            assertTrue(Precision.compareTo(delay.getReal(), lastDelay.getReal(), 1.0e-6) < 0);
            lastDelay = delay;
        }
    }

    @Test
    void testFieldVsNative() {
        doTestFieldVsNative(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldVsNative(final Field<T> field) {
        final T zero = field.getZero();
        Utils.setDataRoot("atmosphere");
        // first line of GPT 2w grid
        PressureTemperatureHumidity pth = new PressureTemperatureHumidity(0.0,
                                                                          101421, 259.2,
                                                                          new CIPM2007().waterVaporPressure(101421, 259.2, 1.64),
                                                                          255.1, 1.665);
        AskneNordiusModel model = new AskneNordiusModel(new DummyMappingFunction());
        HeightDependentPressureTemperatureHumidityConverter converter =
                        new HeightDependentPressureTemperatureHumidityConverter(new CIPM2007());
        for (int h = 0; h < 5000.0; h += 100) {
            for (int e = 0; e < 90; e += 1.0) {
                final double delayN = model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(e), 0.0),
                                                      new GeodeticPoint(0, 0, h),
                                                      converter.convert(pth, h),
                                                      null, AbsoluteDate.J2000_EPOCH).getZw();
                final T delayT = model.pathDelay(new FieldTrackingCoordinates<>(zero,
                                                                                zero.newInstance(FastMath.toRadians(e)),
                                                                                zero),
                                                 new FieldGeodeticPoint<>(zero, zero, zero.newInstance(h)),
                                                 converter.convert(new FieldPressureTemperatureHumidity<>(field, pth),
                                                                   zero.newInstance(h)),
                                                 null, FieldAbsoluteDate.getJ2000Epoch(field)).getZw();
                assertEquals(delayN, delayT.getReal(), 1.0e-6);
            }
        }
    }

}
