/* Copyright 2022-2026 Thales Alenia Space
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
package org.orekit.time.clocks;

import java.util.HashMap;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeInterval;
import org.orekit.utils.drivers.ParameterDriver;

public class ClocksSumTest {

    @Test
    public void testDouble() {
        final AbsoluteDate t0 = AbsoluteDate.ARBITRARY_EPOCH;
        final ClockModel clockModel = new ClocksSum(new ConstantClockModel(1.5),
                                                    new PolynomialClockModel(t0, "dummy",
                                                                             0.0, -1.0, -0.25));
        for (double dt = 0.02; dt < 0.98; dt += 0.02) {
            final ClockOffset co = clockModel.getOffset(t0.shiftedBy(dt));
            Assertions.assertEquals(dt, co.getDate().durationFrom(t0), 1.0e-15);
            Assertions.assertEquals(1.5 - dt * (1.0 + 0.25 * dt), co.getBias(),         1.0e-15);
            Assertions.assertEquals(-1.0 - 0.5 * dt,              co.getRate(),         1.0e-15);
            Assertions.assertEquals(-0.5,                         co.getAcceleration(), 1.0e-15);
        }

    }

    @Test
    public void testField() {
        doTestField(Binary64Field.getInstance());
    }

    public <T extends CalculusFieldElement<T>> void doTestField(final Field<T> field) {
        final AbsoluteDate t0 = AbsoluteDate.ARBITRARY_EPOCH;
        final ClocksSum clockModel = new ClocksSum(new ConstantClockModel(1.5),
                                                   new PolynomialClockModel(t0, "dummy",
                                                                            0.0, -1.0, -0.25));
        final FieldClocksSum<T> fieldClockModel = clockModel.toField(v -> field.getZero().newInstance(v));
        Assertions.assertInstanceOf(ConstantFieldClockModel.class,   fieldClockModel.getClock1());
        Assertions.assertInstanceOf(PolynomialFieldClockModel.class, fieldClockModel.getClock2());
        final FieldAbsoluteDate<T> t0F = new FieldAbsoluteDate<>(field, t0);
        for (double dt = 0.02; dt < 0.98; dt += 0.02) {
            final T dtF = field.getZero().newInstance(dt);
            final FieldClockOffset<T> co = fieldClockModel.getOffset(t0F.shiftedBy(dtF));
            Assertions.assertEquals(dt, co.getDate().durationFrom(t0).getReal(), 1.0e-15);
            Assertions.assertEquals(1.5 - dt * (1.0 + 0.25 * dt), co.getBias().getReal(),       1.0e-15);
            Assertions.assertEquals(-1.0 - 0.5 * dt,              co.getRate().getReal(),         1.0e-15);
            Assertions.assertEquals(-0.5,                         co.getAcceleration().getReal(), 1.0e-15);

            final ClockOffset cn = fieldClockModel.toNonField().getOffset(t0.shiftedBy(dt));
            Assertions.assertEquals(co.getBias().getReal(),         cn.getBias(),         1.0e-15);
            Assertions.assertEquals(co.getRate().getReal(),         cn.getRate(),         1.0e-15);
            Assertions.assertEquals(co.getAcceleration().getReal(), cn.getAcceleration(), 1.0e-15);

        }

        // beware, the result of this test will change when field parameter drivers are available!
        Assertions.assertEquals(AbsoluteDate.PAST_INFINITY,   fieldClockModel.getValidityStart());
        Assertions.assertEquals(AbsoluteDate.FUTURE_INFINITY, fieldClockModel.getValidityEnd());

    }

    @Test
    public void testGetParametersDrivers() {
        final AbsoluteDate t0 = AbsoluteDate.ARBITRARY_EPOCH;
        final ClockModel clockModel = new ClocksSum(new ConstantClockModel(1.5),
                                                    new PolynomialClockModel(t0, "dummy",
                                                                             0.0, -1.0, -0.25));
        List<ParameterDriver> result = clockModel.getParametersDrivers();

        // Should be 4 terms, one for the constant and 3 for the polynomial
        Assertions.assertEquals(4, result.size());
        // Make sure the first term is from the constant
        Assertions.assertEquals(1.5, result.get(0).getValue());
        Assertions.assertEquals(0.0, result.get(1).getValue());
        Assertions.assertEquals(-1.0, result.get(2).getValue());
        Assertions.assertEquals(-0.25, result.get(3).getValue());
    }

    @Test
    void testGetFieldModel() {
        // GIVEN
        final ClockModel c1 = new ConstantClockModel(1.0);
        c1.getParametersDrivers().getFirst().setValidity(TimeInterval.of(AbsoluteDate.QZSS_EPOCH.shiftedBy(-1), 4.0));
        final ClockModel c2 = new ConstantClockModel(4.0);
        c2.getParametersDrivers().getFirst().setValidity(TimeInterval.of(AbsoluteDate.QZSS_EPOCH, 3.0));
        final ClocksSum clockModel = new ClocksSum(c1, c2);
        Assertions.assertEquals(5.0, clockModel.getOffset(AbsoluteDate.QZSS_EPOCH).getBias(), 1.0e-15);
        // WHEN & THEN
        final FieldClocksSum<Gradient> g = clockModel.toGradient(7, new HashMap<>());
        final FieldAbsoluteDate<Gradient> gDate = FieldAbsoluteDate.getArbitraryEpoch(GradientField.getField(7));
        Assertions.assertEquals(7, g.getOffset(gDate).getBias().getFreeParameters());
        Assertions.assertEquals(AbsoluteDate.QZSS_EPOCH, clockModel.getValidityStart());
        Assertions.assertEquals(AbsoluteDate.QZSS_EPOCH.shiftedBy(3.0), clockModel.getValidityEnd());
    }

    @BeforeEach
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

}
