/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.time;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;

public class SampledClockModelTest {

    @Test
    void testCubicNoRate() {
        final PolynomialFunction c       = new PolynomialFunction(FastMath.scalb(1.0, -6),
                                                                  FastMath.scalb(1.0, -7),
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9));
        final PolynomialFunction cDot    = c.polynomialDerivative();
        final PolynomialFunction cDotDot = cDot.polynomialDerivative();
        final AbsoluteDate       t0      = new AbsoluteDate(2020, 4, 1, TimeScalesFactory.getUTC());
        final List<ClockOffset> sample = new ArrayList<>();
        for (double dt = 0; dt < 10; dt += FastMath.scalb(1, -4)) {
            sample.add(new ClockOffset(t0.shiftedBy(dt), c.value(dt), Double.NaN, Double.NaN));
        }
        final SampledClockModel clockModel = new SampledClockModel(sample, 4);
        for (double dt = 0.02; dt < 0.98; dt += 0.02) {
            final ClockOffset co = clockModel.getOffset(t0.shiftedBy(dt));
            assertEquals(dt,                co.getDate().durationFrom(t0), 1.0e-15);
            assertEquals(c.value(dt),       co.getOffset(),                1.0e-15);
            assertEquals(cDot.value(dt),    co.getRate(),                  1.0e-15);
            assertEquals(cDotDot.value(dt), co.getAcceleration(),          1.0e-15);
        }

        assertEquals(t0, clockModel.getCache().getEarliest().getDate());
        assertEquals(t0.shiftedBy(9.9375), clockModel.getCache().getLatest().getDate());
        assertEquals(4, clockModel.getCache().getMaxNeighborsSize());
        assertEquals(160, clockModel.getCache().getAll().size());

    }

    @Test
    void testCubicNoAcceleration() {
        final PolynomialFunction c       = new PolynomialFunction(FastMath.scalb(1.0, -6),
                                                                  FastMath.scalb(1.0, -7),
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9));
        final PolynomialFunction cDot    = c.polynomialDerivative();
        final PolynomialFunction cDotDot = cDot.polynomialDerivative();
        final AbsoluteDate       t0      = new AbsoluteDate(2020, 4, 1, TimeScalesFactory.getUTC());
        final List<ClockOffset> sample = new ArrayList<>();
        for (double dt = 0; dt < 10; dt += FastMath.scalb(1, -4)) {
            sample.add(new ClockOffset(t0.shiftedBy(dt), c.value(dt), cDot.value(dt), Double.NaN));
        }
        final SampledClockModel clockModel = new SampledClockModel(sample, 2);
        for (double dt = 0.02; dt < 0.98; dt += 0.02) {
            final ClockOffset co = clockModel.getOffset(t0.shiftedBy(dt));
            assertEquals(dt,                co.getDate().durationFrom(t0), 1.0e-15);
            assertEquals(c.value(dt),       co.getOffset(),                1.0e-15);
            assertEquals(cDot.value(dt),    co.getRate(),                  1.0e-15);
            assertEquals(cDotDot.value(dt), co.getAcceleration(),          1.0e-15);
        }
    }

    @Test
    void testCubicNoRateField() {
        doTestCubicNoRate(Binary64Field.getInstance());
    }

    public <T extends CalculusFieldElement<T>> void doTestCubicNoRate(final Field<T> field) {
        final PolynomialFunction c       = new PolynomialFunction(FastMath.scalb(1.0, -6),
                                                                  FastMath.scalb(1.0, -7),
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9));
        final PolynomialFunction cDot    = c.polynomialDerivative();
        final PolynomialFunction cDotDot = cDot.polynomialDerivative();
        final AbsoluteDate       t0      = new AbsoluteDate(2020, 4, 1, TimeScalesFactory.getUTC());
        final FieldAbsoluteDate<T> t0F = new FieldAbsoluteDate<>(field, t0);
        final List<ClockOffset> sample = new ArrayList<>();
        for (double dt = 0; dt < 10; dt += FastMath.scalb(1, -4)) {
            sample.add(new ClockOffset(t0.shiftedBy(dt), c.value(dt), Double.NaN, Double.NaN));
        }
        final SampledClockModel clockModel = new SampledClockModel(sample, 4);
        for (double dt = 0.02; dt < 0.98; dt += 0.02) {
            final T dtF = field.getZero().newInstance(dt);
            final FieldClockOffset<T> co = clockModel.getOffset(t0F.shiftedBy(dtF));
            assertEquals(dt, co.getDate().durationFrom(t0).getReal(),                  1.0e-15);
            assertEquals(c.value(dtF).getReal(),       co.getOffset().getReal(),       1.0e-15);
            assertEquals(cDot.value(dtF).getReal(),    co.getRate().getReal(),         1.0e-15);
            assertEquals(cDotDot.value(dtF).getReal(), co.getAcceleration().getReal(), 1.0e-15);
        }
    }

    @Test
    void testCubicNoAccelerationField() {
        doTestCubicNoAcceleration(Binary64Field.getInstance());
    }

    public <T extends CalculusFieldElement<T>> void doTestCubicNoAcceleration(final Field<T> field) {
        final PolynomialFunction c       = new PolynomialFunction(FastMath.scalb(1.0, -6),
                                                                  FastMath.scalb(1.0, -7),
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9));
        final PolynomialFunction cDot    = c.polynomialDerivative();
        final PolynomialFunction cDotDot = cDot.polynomialDerivative();
        final AbsoluteDate       t0      = new AbsoluteDate(2020, 4, 1, TimeScalesFactory.getUTC());
        final FieldAbsoluteDate<T> t0F = new FieldAbsoluteDate<>(field, t0);
        final List<ClockOffset> sample = new ArrayList<>();
        for (double dt = 0; dt < 10; dt += FastMath.scalb(1, -4)) {
            sample.add(new ClockOffset(t0.shiftedBy(dt), c.value(dt), cDot.value(dt), Double.NaN));
        }
        final SampledClockModel clockModel = new SampledClockModel(sample, 2);
        for (double dt = 0.02; dt < 0.98; dt += 0.02) {
            final T dtF = field.getZero().newInstance(dt);
            final FieldClockOffset<T> co = clockModel.getOffset(t0F.shiftedBy(dtF));
            assertEquals(dt, co.getDate().durationFrom(t0).getReal(),                  1.0e-15);
            assertEquals(c.value(dtF).getReal(),       co.getOffset().getReal(),       1.0e-15);
            assertEquals(cDot.value(dtF).getReal(),    co.getRate().getReal(),         1.0e-15);
            assertEquals(cDotDot.value(dtF).getReal(), co.getAcceleration().getReal(), 1.0e-15);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

}
