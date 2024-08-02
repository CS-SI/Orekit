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
package org.orekit.estimation.measurements;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;

class QuadraticClockModelTest {

    @Test
    void testValue() {
        final AbsoluteDate        t0    = AbsoluteDate.GALILEO_EPOCH;
        final QuadraticClockModel clock = new QuadraticClockModel(t0,
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9),
                                                                  FastMath.scalb(1.0, -10));
        assertEquals(1.00 / 256.0, clock.getOffset(t0).getOffset(),                1.0e-15);
        assertEquals(1.75 / 256.0, clock.getOffset(t0.shiftedBy(1.0)).getOffset(), 1.0e-15);
        assertEquals(3.00 / 256.0, clock.getOffset(t0.shiftedBy(2.0)).getOffset(), 1.0e-15);
    }

    @Test
    void testValueField() {
        final AbsoluteDate        t0    = AbsoluteDate.GALILEO_EPOCH;
        final QuadraticClockModel clock = new QuadraticClockModel(t0,
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9),
                                                                  FastMath.scalb(1.0, -10));
        final FieldAbsoluteDate<Binary64> t064 = new FieldAbsoluteDate<>(Binary64Field.getInstance(), t0);
        assertEquals(1.00 / 256.0, clock.getOffset(t064).getOffset().getReal(),                1.0e-15);
        assertEquals(1.75 / 256.0, clock.getOffset(t064.shiftedBy(1.0)).getOffset().getReal(), 1.0e-15);
        assertEquals(3.00 / 256.0, clock.getOffset(t064.shiftedBy(2.0)).getOffset().getReal(), 1.0e-15);
    }

    @Test
    void testRate() {
        final AbsoluteDate        t0    = AbsoluteDate.GALILEO_EPOCH;
        final QuadraticClockModel clock = new QuadraticClockModel(t0,
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9),
                                                                  FastMath.scalb(1.0, -10));
        assertEquals(1.00 / 512, clock.getOffset(t0).getRate(),                1.0e-15);
        assertEquals(2.00 / 512, clock.getOffset(t0.shiftedBy(1.0)).getRate(), 1.0e-15);
        assertEquals(3.00 / 512, clock.getOffset(t0.shiftedBy(2.0)).getRate(), 1.0e-15);
    }

    @Test
    void testRateField() {
        final AbsoluteDate        t0    = AbsoluteDate.GALILEO_EPOCH;
        final QuadraticClockModel clock = new QuadraticClockModel(t0,
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9),
                                                                  FastMath.scalb(1.0, -10));
        final FieldAbsoluteDate<Binary64> t064 = new FieldAbsoluteDate<>(Binary64Field.getInstance(), t0);
        assertEquals(1.00 / 512, clock.getOffset(t064).getRate().getReal(),                1.0e-15);
        assertEquals(2.00 / 512, clock.getOffset(t064.shiftedBy(1.0)).getRate().getReal(), 1.0e-15);
        assertEquals(3.00 / 512, clock.getOffset(t064.shiftedBy(2.0)).getRate().getReal(), 1.0e-15);
    }

    @Test
    void testAcceleration() {
        final AbsoluteDate        t0    = AbsoluteDate.GALILEO_EPOCH;
        final QuadraticClockModel clock = new QuadraticClockModel(t0,
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9),
                                                                  FastMath.scalb(1.0, -10));
        assertEquals(2.00 / 1024, clock.getOffset(t0).getAcceleration(),                1.0e-15);
        assertEquals(2.00 / 1024, clock.getOffset(t0.shiftedBy(1.0)).getAcceleration(), 1.0e-15);
        assertEquals(2.00 / 1024, clock.getOffset(t0.shiftedBy(2.0)).getAcceleration(), 1.0e-15);
    }

    @Test
    void testAccelerationField() {
        final AbsoluteDate        t0    = AbsoluteDate.GALILEO_EPOCH;
        final QuadraticClockModel clock = new QuadraticClockModel(t0,
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9),
                                                                  FastMath.scalb(1.0, -10));
        final FieldAbsoluteDate<Binary64> t064 = new FieldAbsoluteDate<>(Binary64Field.getInstance(), t0);
        assertEquals(2.00 / 1024, clock.getOffset(t064).getAcceleration().getReal(),                1.0e-15);
        assertEquals(2.00 / 1024, clock.getOffset(t064.shiftedBy(1.0)).getAcceleration().getReal(), 1.0e-15);
        assertEquals(2.00 / 1024, clock.getOffset(t064.shiftedBy(2.0)).getAcceleration().getReal(), 1.0e-15);
    }

    @Test
    void testValidity() {
        final AbsoluteDate        t0    = AbsoluteDate.GALILEO_EPOCH;
        final QuadraticClockModel clock = new QuadraticClockModel(t0,
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9),
                                                                  FastMath.scalb(1.0, -10));
        assertEquals(AbsoluteDate.PAST_INFINITY, clock.getValidityStart());
        assertEquals(AbsoluteDate.FUTURE_INFINITY, clock.getValidityEnd());
    }

    @Test
    void testSafeReferenceDate() {
        final ParameterDriver a0 = new ParameterDriver("a0", 0.0, 1.0,
                                                       Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        final ParameterDriver a1 = new ParameterDriver("a1", 0.0, 1.0,
                                                       Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        final ParameterDriver a2 = new ParameterDriver("a2", 0.0, 1.0,
                                                       Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        final QuadraticClockModel clock = new QuadraticClockModel(a0, a1, a2);

        // OK to have no reference date if a1 and a2 are both zero
        a0.setValue(0.125);
        assertEquals(0.125, clock.getOffset(AbsoluteDate.GALILEO_EPOCH).getOffset());

        // not OK to have no reference date if a1 is non zero
        a1.setValue(1.0);
        try {
            clock.getOffset(AbsoluteDate.GALILEO_EPOCH);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER, oe.getSpecifier());
            assertEquals(a0.getName(), oe.getParts()[0]);
        }

        // not OK to have no reference date if a2 is non zero
        a1.setValue(0.0);
        a2.setValue(1.0);
        try {
            clock.getOffset(AbsoluteDate.GALILEO_EPOCH);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER, oe.getSpecifier());
            assertEquals(a0.getName(), oe.getParts()[0]);
        }

        // back to OK if we reset drift and acceleration
        a2.setValue(0);
        assertEquals(0.125, clock.getOffset(AbsoluteDate.GALILEO_EPOCH).getOffset());

    }

    @Test
    void testGradient() {
        final AbsoluteDate    t0 = AbsoluteDate.GALILEO_EPOCH;
        final ParameterDriver a0 = new ParameterDriver("a0", 0.0, 1.0,
                                                       Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        final ParameterDriver a1 = new ParameterDriver("a1", 0.0, 1.0,
                                                       Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        final ParameterDriver a2 = new ParameterDriver("a2", 0.0, 1.0,
                                                       Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        final QuadraticClockModel clock = new QuadraticClockModel(a0, a1, a2);

        int nbParams = 0;
        final Map<String, Integer> indices = new HashMap<>();
        a0.setValue(FastMath.scalb(1.0, -8));
        a0.setReferenceDate(t0);
        a0.setSelected(true);
        for (Span<String> span = a0.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
            indices.put(span.getData(), nbParams++);
        }
        a1.setValue(FastMath.scalb(1.0, -9));
        a1.setReferenceDate(t0);
        a1.setSelected(true);
        for (Span<String> span = a1.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
            indices.put(span.getData(), nbParams++);
        }
        a2.setValue(FastMath.scalb(1.0, -10));
        a2.setReferenceDate(t0);
        a2.setSelected(true);
        for (Span<String> span = a2.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
            indices.put(span.getData(), nbParams++);
        }

        QuadraticFieldClockModel<Gradient> gradientModel = clock.toGradientModel(nbParams, indices, t0);
        final FieldAbsoluteDate<Gradient> t0g = new FieldAbsoluteDate<>(GradientField.getField(nbParams), t0);

        final Gradient g0 = gradientModel.getOffset(t0g).getOffset();
        assertEquals(1.00 / 256, g0.getValue(), 1.0e-15);
        assertEquals(1.00,       g0.getPartialDerivative(0), 1.0e-15);
        assertEquals(0.00,       g0.getPartialDerivative(1), 1.0e-15);
        assertEquals(0.00,       g0.getPartialDerivative(2), 1.0e-15);

        final Gradient g1 = gradientModel.getOffset(t0g.shiftedBy(1.0)).getOffset();
        assertEquals(1.75 / 256, g1.getValue(), 1.0e-15);
        assertEquals(1.00,       g1.getPartialDerivative(0), 1.0e-15);
        assertEquals(1.00,       g1.getPartialDerivative(1), 1.0e-15);
        assertEquals(1.00,       g1.getPartialDerivative(2), 1.0e-15);

        final Gradient g2 = gradientModel.getOffset(t0g.shiftedBy(2.0)).getOffset();
        assertEquals(3.00 / 256, g2.getValue(), 1.0e-15);
        assertEquals(1.00,       g2.getPartialDerivative(0), 1.0e-15);
        assertEquals(2.00,       g2.getPartialDerivative(1), 1.0e-15);
        assertEquals(4.00,       g2.getPartialDerivative(2), 1.0e-15);

    }

}
