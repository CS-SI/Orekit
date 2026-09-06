/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.utils.drivers;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.UnsupportedParameterException;
import org.orekit.time.TimeInterval;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class FieldParameterDriversProviderTest {

    @Test
    void testFindByName() {
        doTestFindByName(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFindByName(final Field<T> field) {
        final FieldParameterDriver<T> driver =
            new FieldParameterDriver<>("a", field.getZero(), 1.0, -1.0, 1.0, TimeInterval.UNLIMITED);
        final List<FieldParameterDriver<T>> drivers = Collections.singletonList(driver);
        Assertions.assertTrue(BaseParameterDriversProvider.findByName(drivers, "a"));
        Assertions.assertFalse(BaseParameterDriversProvider.findByName(drivers, "A"));
    }

    @Test
    void testGetParameterDriverWithSubstring() {
        doTestGetParameterDriverWithSubstring(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestGetParameterDriverWithSubstring(final Field<T> field) {
        final FieldParameterDriver<T> driverFirst =
            new FieldParameterDriver<>("clock-bias", field.getZero(), 1.0, -1.0, 1.0, TimeInterval.UNLIMITED);
        final FieldParameterDriver<T> driverSecond =
            new FieldParameterDriver<>("clock-drift", field.getZero(), 1.0, -1.0, 1.0, TimeInterval.UNLIMITED);
        final FieldParameterDriversProvider<T> provider = new FieldParameterDriversProvider<>() {
            /** {@inheritDoc} */
            @Override
            public List<FieldParameterDriver<T>> getParametersDrivers() {
                return Arrays.asList(driverFirst, driverSecond);
            }
        };

        Assertions.assertSame(driverFirst, provider.getParameterDriverWithSubstring("bias"));
        Assertions.assertSame(driverSecond, provider.getParameterDriverWithSubstring("drift"));
        Assertions.assertThrows(UnsupportedParameterException.class,
                                () -> provider.getParameterDriverWithSubstring("nonexistent"));

    }

    @Test
    void testGetParameterDriverWithSubstringMultipleMatches() {
        doTestGetParameterDriverWithSubstringMultipleMatches(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestGetParameterDriverWithSubstringMultipleMatches(final Field<T> field) {
        final FieldParameterDriver<T> driverFirst =
            new FieldParameterDriver<>("satellite-clock-bias", field.getZero(), 1.0, -1.0, 1.0, TimeInterval.UNLIMITED);
        final FieldParameterDriver<T> driverSecond =
            new FieldParameterDriver<>("station-clock-bias", field.getOne(), 1.0, -1.0, 1.0, TimeInterval.UNLIMITED);
        final FieldParameterDriversProvider<T> provider = new FieldParameterDriversProvider<>() {
            /** {@inheritDoc} */
            @Override
            public List<FieldParameterDriver<T>> getParametersDrivers() {
                return Arrays.asList(driverFirst, driverSecond);
            }
        };
        Assertions.assertEquals(2,  provider.getParameters().length);
        Assertions.assertSame(field.getZero(),  provider.getParameters()[0]);
        Assertions.assertSame(field.getOne(),  provider.getParameters()[1]);

        // Test substring that matches both drivers throws exception
        Assertions.assertThrows(UnsupportedParameterException.class,
                () -> provider.getParameterDriverWithSubstring("clock-bias"),
                "Should throw exception when multiple drivers match substring");

        // Test unique substring returns correct driver
        Assertions.assertSame(driverFirst, provider.getParameterDriverWithSubstring("satellite"));
        Assertions.assertSame(driverSecond, provider.getParameterDriverWithSubstring("station"));

    }

}
