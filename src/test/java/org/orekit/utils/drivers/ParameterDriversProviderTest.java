/* Copyright 2022-2026 Romain Serra
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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.errors.UnsupportedParameterException;

class ParameterDriversProviderTest {

    @Test
    void testFindByName() {
        // GIVEN
        final ParameterDriver driver = Mockito.mock(ParameterDriver.class);
        final String expectedName = "a";
        Mockito.when(driver.getName()).thenReturn(expectedName);
        final List<ParameterDriver> drivers = new ArrayList<>();
        drivers.add(driver);
        // WHEN
        final boolean found = ParameterDriversProvider.findByName(drivers, expectedName);
        // THEN
        Assertions.assertTrue(found);
        Assertions.assertFalse(ParameterDriversProvider.findByName(drivers, expectedName.toUpperCase()));
    }

    @Test
    void testGetParameterDriverWithSubstring() {
        // GIVEN
        final ParameterDriver driverFirst = Mockito.mock(ParameterDriver.class);
        final String expectedFirstName = "clock-bias";
        Mockito.when(driverFirst.getName()).thenReturn(expectedFirstName);

        final ParameterDriver driverSecond = Mockito.mock(ParameterDriver.class);
        final String expectedSecondName = "clock-drift";
        Mockito.when(driverSecond.getName()).thenReturn(expectedSecondName);

        final List<ParameterDriver> drivers = new ArrayList<>();
        drivers.add(driverFirst);
        drivers.add(driverSecond);

        final ParameterDriversProvider provider = Mockito.mock(ParameterDriversProvider.class);
        Mockito.when(provider.getParametersDrivers()).thenReturn(drivers);
        Mockito.when(provider.getParameterDriverWithSubstring(Mockito.anyString())).thenCallRealMethod();

        // WHEN
        final ParameterDriver found = provider.getParameterDriverWithSubstring("bias");
        // THEN
        Assertions.assertEquals(driverFirst, found);

        // Test finding second driver
        final ParameterDriver foundSecond = provider.getParameterDriverWithSubstring("drift");
        Assertions.assertEquals(driverSecond, foundSecond);

        // Test substring that doesn't exist throws exception
        Assertions.assertThrows(UnsupportedParameterException.class,
                () -> provider.getParameterDriverWithSubstring("nonexistent"));
    }

    @Test
    void testGetParameterDriverWithSubstringMultipleMatches() {
        // GIVEN
        final ParameterDriver driverFirst = Mockito.mock(ParameterDriver.class);
        final String firstName = "satellite-clock-bias";
        Mockito.when(driverFirst.getName()).thenReturn(firstName);

        final ParameterDriver driverSecond = Mockito.mock(ParameterDriver.class);
        final String secondName = "station-clock-bias";
        Mockito.when(driverSecond.getName()).thenReturn(secondName);

        final List<ParameterDriver> drivers = new ArrayList<>();
        drivers.add(driverFirst);
        drivers.add(driverSecond);

        final ParameterDriversProvider provider = Mockito.mock(ParameterDriversProvider.class);
        Mockito.when(provider.getParametersDrivers()).thenReturn(drivers);
        Mockito.when(provider.getParameterDriverWithSubstring(Mockito.anyString())).thenCallRealMethod();

        // WHEN & THEN
        // Test substring that matches both drivers throws exception
        Assertions.assertThrows(UnsupportedParameterException.class,
                () -> provider.getParameterDriverWithSubstring("clock-bias"),
                "Should throw exception when multiple drivers match substring");

        // Test unique substring returns correct driver
        final ParameterDriver foundSatellite = provider.getParameterDriverWithSubstring("satellite");
        Assertions.assertEquals(driverFirst, foundSatellite);

        final ParameterDriver foundStation = provider.getParameterDriverWithSubstring("station");
        Assertions.assertEquals(driverSecond, foundStation);
    }
}
