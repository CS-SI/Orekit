/* Copyright 2025-2026 Hawkeye 360 (HE360)
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.time.clocks;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.errors.UnsupportedParameterException;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.drivers.ParameterDriver;

public class ClockModelTest {

    @Test
    public void testGetAcceptedTermName() {
        // Test the default getAcceptedTermName method using a mock
        final ClockModel clock = Mockito.mock(ClockModel.class, Mockito.CALLS_REAL_METHODS);

        // Test standard term names (indices 0-2)
        Assertions.assertEquals("-clock-bias", clock.getAcceptedTermSuffix(0),
                "Index 0 should be bias");
        Assertions.assertEquals("-clock-drift", clock.getAcceptedTermSuffix(1),
                "Index 1 should be drift");
        Assertions.assertEquals("-clock-acceleration", clock.getAcceptedTermSuffix(2),
                "Index 2 should be acceleration");

        // Test higher-order term names (index >= 3)
        Assertions.assertEquals("-clock-term-3", clock.getAcceptedTermSuffix(3),
                "Index 3 should be clock-term-3");
        Assertions.assertEquals("-clock-term-4", clock.getAcceptedTermSuffix(4),
                "Index 4 should be clock-term-4");
        Assertions.assertEquals("-clock-term-10", clock.getAcceptedTermSuffix(10),
                "Index 10 should be clock-term-10");
        Assertions.assertEquals("-clock-term-100", clock.getAcceptedTermSuffix(100),
                "Index 100 should be clock-term-100");
    }

    @Test
    public void testGetOffsetValue() {
        // Test the default getOffsetValue method using a mock
        final ClockModel clock = Mockito.mock(ClockModel.class, Mockito.CALLS_REAL_METHODS);
        final AbsoluteDate t0 = AbsoluteDate.GPS_EPOCH;
        final AbsoluteDate t1 = t0.shiftedBy(100.0);

        // Mock the getOffset method that getOffsetValue depends on
        final ClockOffset offset0 = Mockito.mock(ClockOffset.class);
        final ClockOffset offset1 = Mockito.mock(ClockOffset.class);

        Mockito.when(clock.getOffset(t0)).thenReturn(offset0);
        Mockito.when(clock.getOffset(t1)).thenReturn(offset1);

        Mockito.when(offset0.getValue(t0)).thenReturn(1.5e-7);
        Mockito.when(offset1.getValue(t1)).thenReturn(2.8e-7);

        // Test that getOffsetValue delegates to getOffset().getValue()
        double value0 = clock.getOffsetValue(t0);
        Assertions.assertEquals(1.5e-7, value0, 1.0e-15,
                "getOffsetValue should delegate to getOffset().getValue()");

        double value1 = clock.getOffsetValue(t1);
        Assertions.assertEquals(2.8e-7, value1, 1.0e-15,
                "getOffsetValue should delegate to getOffset().getValue()");

        // Verify the method calls
        Mockito.verify(clock).getOffset(t0);
        Mockito.verify(offset0).getValue(t0);
        Mockito.verify(clock).getOffset(t1);
        Mockito.verify(offset1).getValue(t1);
    }

    @Test
    public void testGetBiasDriver() {
        // Test getBiasDriver default method using a mock
        final ClockModel clock = Mockito.mock(ClockModel.class, Mockito.CALLS_REAL_METHODS);

        // Create mock parameter drivers
        final ParameterDriver biasDriver = Mockito.mock(ParameterDriver.class);
        Mockito.when(biasDriver.getName()).thenReturn("satellite-clock-bias");

        final List<ParameterDriver> drivers = new ArrayList<>();
        drivers.add(biasDriver);

        // Mock getParametersDrivers method
        Mockito.when(clock.getParametersDrivers()).thenReturn(drivers);

        // Mock getParameterDriverWithSubstring to call real method
        Mockito.when(clock.getParameterDriverWithSubstring(Mockito.anyString())).thenCallRealMethod();

        // Test getBiasDriver
        ParameterDriver result = clock.getBiasDriver();
        Assertions.assertEquals(biasDriver, result,
                "getBiasDriver should return driver with '-clock-bias' in name");

    }

    @Test
    public void testGetRateDriver() {
        // Test getRateDriver default method using a mock
        final ClockModel clock = Mockito.mock(ClockModel.class, Mockito.CALLS_REAL_METHODS);

        // Create mock parameter drivers
        final ParameterDriver rateDriver = Mockito.mock(ParameterDriver.class);
        Mockito.when(rateDriver.getName()).thenReturn("station-clock-drift");

        final List<ParameterDriver> drivers = new ArrayList<>();
        drivers.add(rateDriver);

        // Mock getParametersDrivers method
        Mockito.when(clock.getParametersDrivers()).thenReturn(drivers);

        // Mock getParameterDriverWithSubstring to call real method
        Mockito.when(clock.getParameterDriverWithSubstring(Mockito.anyString())).thenCallRealMethod();

        // Test getRateDriver
        ParameterDriver result = clock.getRateDriver();
        Assertions.assertEquals(rateDriver, result,
                "getRateDriver should return driver with '-clock-drift' in name");

    }

    @Test
    public void testGetAccelerationDriver() {
        // Test getAccelerationDriver default method using a mock
        final ClockModel clock = Mockito.mock(ClockModel.class, Mockito.CALLS_REAL_METHODS);

        // Create mock parameter drivers
        final ParameterDriver accelDriver = Mockito.mock(ParameterDriver.class);
        Mockito.when(accelDriver.getName()).thenReturn("sat123-clock-acceleration");

        final List<ParameterDriver> drivers = new ArrayList<>();
        drivers.add(accelDriver);

        // Mock getParametersDrivers method
        Mockito.when(clock.getParametersDrivers()).thenReturn(drivers);

        // Mock getParameterDriverWithSubstring to call real method
        Mockito.when(clock.getParameterDriverWithSubstring(Mockito.anyString())).thenCallRealMethod();

        // Test getAccelerationDriver
        ParameterDriver result = clock.getAccelerationDriver();
        Assertions.assertEquals(accelDriver, result,
                "getAccelerationDriver should return driver with '-clock-acceleration' in name");

    }

    @Test
    public void testGetBiasDriverNotAvailable() {
        // Test when bias driver is not available
        final ClockModel clock = Mockito.mock(ClockModel.class, Mockito.CALLS_REAL_METHODS);

        // Create a driver with non-standard name
        final ParameterDriver otherDriver = Mockito.mock(ParameterDriver.class);
        Mockito.when(otherDriver.getName()).thenReturn("other-parameter");

        final List<ParameterDriver> drivers = new ArrayList<>();
        drivers.add(otherDriver);

        // Mock getParametersDrivers method
        Mockito.when(clock.getParametersDrivers()).thenReturn(drivers);

        // Mock getParameterDriverWithSubstring to call real method
        Mockito.when(clock.getParameterDriverWithSubstring(Mockito.anyString())).thenCallRealMethod();

        // Should throw exception when bias driver not found
        Assertions.assertThrows(UnsupportedParameterException.class,
                () -> clock.getBiasDriver(),
                "Should throw exception when bias driver not available");
    }

    @Test
    public void testGetParameterDriverTermIndexZero() {
        // Test getParameterDriverTerm for index 0 (should call getBiasDriver)
        final ClockModel clock = Mockito.mock(ClockModel.class, Mockito.CALLS_REAL_METHODS);

        final ParameterDriver biasDriver = Mockito.mock(ParameterDriver.class);
        Mockito.when(biasDriver.getName()).thenReturn("-clock-bias");

        // Mock getBiasDriver using doReturn to avoid calling real method during stubbing
        Mockito.doReturn(biasDriver).when(clock).getBiasDriver();

        // Test index 0
        ParameterDriver result = clock.getParameterDriverTerm(0);
        Assertions.assertEquals(biasDriver, result,
                "getParameterDriverTerm(0) should delegate to getBiasDriver()");

        // Verify getBiasDriver was called
        Mockito.verify(clock).getBiasDriver();
    }

    @Test
    public void testGetParameterDriverTermIndexOne() {
        // Test getParameterDriverTerm for index 1 (should call getRateDriver)
        final ClockModel clock = Mockito.mock(ClockModel.class, Mockito.CALLS_REAL_METHODS);

        final ParameterDriver rateDriver = Mockito.mock(ParameterDriver.class);
        Mockito.when(rateDriver.getName()).thenReturn("-clock-drift");

        // Mock getRateDriver using doReturn to avoid calling real method during stubbing
        Mockito.doReturn(rateDriver).when(clock).getRateDriver();

        // Test index 1
        ParameterDriver result = clock.getParameterDriverTerm(1);
        Assertions.assertEquals(rateDriver, result,
                "getParameterDriverTerm(1) should delegate to getRateDriver()");

        // Verify getRateDriver was called
        Mockito.verify(clock).getRateDriver();
    }

    @Test
    public void testGetParameterDriverTermIndexTwo() {
        // Test getParameterDriverTerm for index 2 (should call getAccelerationDriver)
        final ClockModel clock = Mockito.mock(ClockModel.class, Mockito.CALLS_REAL_METHODS);

        final ParameterDriver accelDriver = Mockito.mock(ParameterDriver.class);
        Mockito.when(accelDriver.getName()).thenReturn("-clock-acceleration");

        // Mock getAccelerationDriver using doReturn to avoid calling real method during stubbing
        Mockito.doReturn(accelDriver).when(clock).getAccelerationDriver();

        // Test index 2
        ParameterDriver result = clock.getParameterDriverTerm(2);
        Assertions.assertEquals(accelDriver, result,
                "getParameterDriverTerm(2) should delegate to getAccelerationDriver()");

        // Verify getAccelerationDriver was called
        Mockito.verify(clock).getAccelerationDriver();
    }

    @Test
    public void testGetParameterDriverTermHigherOrder() {
        // Test getParameterDriverTerm for higher-order terms (should call getParameterDriver)
        final ClockModel clock = Mockito.mock(ClockModel.class, Mockito.CALLS_REAL_METHODS);

        final ParameterDriver term4Driver = Mockito.mock(ParameterDriver.class);
        Mockito.when(term4Driver.getName()).thenReturn("-clock-term-4");

        // Mock getParameterDriver using doReturn to avoid calling real method during stubbing
        Mockito.doReturn(term4Driver).when(clock).getParameterDriver("-clock-term-4");

        // Test index 4
        ParameterDriver result = clock.getParameterDriverTerm(4);
        Assertions.assertEquals(term4Driver, result,
                "getParameterDriverTerm(4) should call getParameterDriver with correct name");

        // Verify getParameterDriver was called with correct name
        Mockito.verify(clock).getParameterDriver("-clock-term-4");
    }

    @Test
    public void testGetParameterDriverTermSwitchCases() {
        // Test that the switch statement correctly routes to different methods
        final ClockModel clock = Mockito.mock(ClockModel.class, Mockito.CALLS_REAL_METHODS);

        final ParameterDriver bias = Mockito.mock(ParameterDriver.class);
        final ParameterDriver rate = Mockito.mock(ParameterDriver.class);
        final ParameterDriver accel = Mockito.mock(ParameterDriver.class);
        final ParameterDriver term5 = Mockito.mock(ParameterDriver.class);

        // Use doReturn to avoid calling real methods during stubbing
        Mockito.doReturn(bias).when(clock).getBiasDriver();
        Mockito.doReturn(rate).when(clock).getRateDriver();
        Mockito.doReturn(accel).when(clock).getAccelerationDriver();
        Mockito.doReturn(term5).when(clock).getParameterDriver("-clock-term-5");

        // Test all switch cases
        Assertions.assertEquals(bias, clock.getParameterDriverTerm(0));
        Assertions.assertEquals(rate, clock.getParameterDriverTerm(1));
        Assertions.assertEquals(accel, clock.getParameterDriverTerm(2));
        Assertions.assertEquals(term5, clock.getParameterDriverTerm(5));
    }
}
