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
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;

class PolynomialClockModelTest {

    @Test
    void testValue() {
        final AbsoluteDate        t0    = AbsoluteDate.GALILEO_EPOCH;
        final PolynomialClockModel clock = new PolynomialClockModel(t0,
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9),
                                                                  FastMath.scalb(1.0, -10));
        Assertions.assertEquals(1.00 / 256.0, clock.getOffset(t0).getBias(),                1.0e-15);
        Assertions.assertEquals(1.75 / 256.0, clock.getOffset(t0.shiftedBy(1.0)).getBias(), 1.0e-15);
        Assertions.assertEquals(3.00 / 256.0, clock.getOffset(t0.shiftedBy(2.0)).getBias(), 1.0e-15);
    }

    @Test
    void testValueField() {
        final AbsoluteDate        t0    = AbsoluteDate.GALILEO_EPOCH;
        final PolynomialClockModel clock = new PolynomialClockModel(t0,
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9),
                                                                  FastMath.scalb(1.0, -10));
        final FieldAbsoluteDate<Binary64> t064 = new FieldAbsoluteDate<>(Binary64Field.getInstance(), t0);
        Assertions.assertEquals(1.00 / 256.0, clock.getFieldOffset(t064).getBias().getReal(),                1.0e-15);
        Assertions.assertEquals(1.75 / 256.0, clock.getFieldOffset(t064.shiftedBy(1.0)).getBias().getReal(), 1.0e-15);
        Assertions.assertEquals(3.00 / 256.0, clock.getFieldOffset(t064.shiftedBy(2.0)).getBias().getReal(), 1.0e-15);
    }

    @Test
    void testRate() {
        final AbsoluteDate        t0    = AbsoluteDate.GALILEO_EPOCH;
        final PolynomialClockModel clock = new PolynomialClockModel(t0,
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9),
                                                                  FastMath.scalb(1.0, -10));
        Assertions.assertEquals(1.00 / 512, clock.getOffset(t0).getRate(),                1.0e-15);
        Assertions.assertEquals(2.00 / 512, clock.getOffset(t0.shiftedBy(1.0)).getRate(), 1.0e-15);
        Assertions.assertEquals(3.00 / 512, clock.getOffset(t0.shiftedBy(2.0)).getRate(), 1.0e-15);
    }

    @Test
    void testRateField() {
        final AbsoluteDate        t0    = AbsoluteDate.GALILEO_EPOCH;
        final PolynomialClockModel clock = new PolynomialClockModel(t0,
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9),
                                                                  FastMath.scalb(1.0, -10));
        final FieldAbsoluteDate<Binary64> t064 = new FieldAbsoluteDate<>(Binary64Field.getInstance(), t0);
        Assertions.assertEquals(1.00 / 512, clock.getFieldOffset(t064).getRate().getReal(),                1.0e-15);
        Assertions.assertEquals(2.00 / 512, clock.getFieldOffset(t064.shiftedBy(1.0)).getRate().getReal(), 1.0e-15);
        Assertions.assertEquals(3.00 / 512, clock.getFieldOffset(t064.shiftedBy(2.0)).getRate().getReal(), 1.0e-15);
    }

    @Test
    void testAcceleration() {
        final AbsoluteDate        t0    = AbsoluteDate.GALILEO_EPOCH;
        final PolynomialClockModel clock = new PolynomialClockModel(t0,
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9),
                                                                  FastMath.scalb(1.0, -10));
        Assertions.assertEquals(2.00 / 1024, clock.getOffset(t0).getAcceleration(),                1.0e-15);
        Assertions.assertEquals(2.00 / 1024, clock.getOffset(t0.shiftedBy(1.0)).getAcceleration(), 1.0e-15);
        Assertions.assertEquals(2.00 / 1024, clock.getOffset(t0.shiftedBy(2.0)).getAcceleration(), 1.0e-15);
    }

    @Test
    void testAccelerationField() {
        final AbsoluteDate        t0    = AbsoluteDate.GALILEO_EPOCH;
        final PolynomialClockModel clock = new PolynomialClockModel(t0,
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9),
                                                                  FastMath.scalb(1.0, -10));
        final FieldAbsoluteDate<Binary64> t064 = new FieldAbsoluteDate<>(Binary64Field.getInstance(), t0);
        Assertions.assertEquals(2.00 / 1024, clock.getFieldOffset(t064).getAcceleration().getReal(),                1.0e-15);
        Assertions.assertEquals(2.00 / 1024, clock.getFieldOffset(t064.shiftedBy(1.0)).getAcceleration().getReal(), 1.0e-15);
        Assertions.assertEquals(2.00 / 1024, clock.getFieldOffset(t064.shiftedBy(2.0)).getAcceleration().getReal(), 1.0e-15);
    }

    @Test
    void testValidity() {
        final AbsoluteDate        t0    = AbsoluteDate.GALILEO_EPOCH;
        final PolynomialClockModel clock = new PolynomialClockModel(t0,
                                                                  FastMath.scalb(1.0, -8),
                                                                  FastMath.scalb(1.0, -9),
                                                                  FastMath.scalb(1.0, -10));
        Assertions.assertEquals(AbsoluteDate.PAST_INFINITY, clock.getValidityStart());
        Assertions.assertEquals(AbsoluteDate.FUTURE_INFINITY, clock.getValidityEnd());
    }

    @Test
    void testSafeReferenceDate() {
        final ParameterDriver a0 = new ParameterDriver("-clock-bias", 0.0, 1.0,
                                                       Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                       AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        final ParameterDriver a1 = new ParameterDriver("-clock-drift", 0.0, 1.0,
                                                       Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                       AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        final ParameterDriver a2 = new ParameterDriver("-clock-acceleration", 0.0, 1.0,
                                                       Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                       AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        final PolynomialClockModel clock = new PolynomialClockModel(List.of(a0, a1, a2));

        // not OK to have no reference date
        a0.setValue(0.125);
        try {
            clock.getOffset(AbsoluteDate.GALILEO_EPOCH);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER, oe.getSpecifier());
            Assertions.assertEquals(a0.getName(), oe.getParts()[0]);
        }

        // not OK to have no reference date if a2 is non zero
        a1.setValue(0.0);
        a2.setValue(1.0);
        try {
            clock.getOffset(AbsoluteDate.GALILEO_EPOCH);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER, oe.getSpecifier());
            Assertions.assertEquals(a0.getName(), oe.getParts()[0]);
        }

        // back to OK if we reset drift and acceleration
        a0.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);
        Assertions.assertEquals(0.125, clock.getOffset(AbsoluteDate.GALILEO_EPOCH).getBias());

    }

    @Test
    void testGradient() {
        final AbsoluteDate    t0 = AbsoluteDate.GALILEO_EPOCH;
        final ParameterDriver a0 = new ParameterDriver("-clock-bias", 0.0, 1.0,
                                                       Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                       AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        final ParameterDriver a1 = new ParameterDriver("-clock-drift", 0.0, 1.0,
                                                       Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                       AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        final ParameterDriver a2 = new ParameterDriver("-clock-acceleration", 0.0, 1.0,
                                                       Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                       AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        final PolynomialClockModel clock = new PolynomialClockModel(a0, a1, a2);

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

        FieldClockModel<Gradient> gradientModel = clock.getFieldModel(nbParams, indices, t0);
        final FieldAbsoluteDate<Gradient> t0g = new FieldAbsoluteDate<>(GradientField.getField(nbParams), t0);

        final Gradient g0 = gradientModel.getOffset(t0g).getBias();
        Assertions.assertEquals(1.00 / 256, g0.getValue(), 1.0e-15);
        Assertions.assertEquals(1.00,       g0.getPartialDerivative(0), 1.0e-15);
        Assertions.assertEquals(0.00,       g0.getPartialDerivative(1), 1.0e-15);
        Assertions.assertEquals(0.00,       g0.getPartialDerivative(2), 1.0e-15);

        final Gradient g1 = gradientModel.getOffset(t0g.shiftedBy(1.0)).getBias();
        Assertions.assertEquals(1.75 / 256, g1.getValue(), 1.0e-15);
        Assertions.assertEquals(1.00,       g1.getPartialDerivative(0), 1.0e-15);
        Assertions.assertEquals(1.00,       g1.getPartialDerivative(1), 1.0e-15);
        Assertions.assertEquals(1.00,       g1.getPartialDerivative(2), 1.0e-15);

        final Gradient g2 = gradientModel.getOffset(t0g.shiftedBy(2.0)).getBias();
        Assertions.assertEquals(3.00 / 256, g2.getValue(), 1.0e-15);
        Assertions.assertEquals(1.00,       g2.getPartialDerivative(0), 1.0e-15);
        Assertions.assertEquals(2.00,       g2.getPartialDerivative(1), 1.0e-15);
        Assertions.assertEquals(4.00,       g2.getPartialDerivative(2), 1.0e-15);

    }

    @Test
    void testPerfectClock() {
        // A perfect clock has zero offset, rate, and acceleration at all times
        final AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        final PolynomialClockModel perfectClock = new PolynomialClockModel(t0, 0.0);

        // Test at reference time
        ClockOffset offset0 = perfectClock.getOffset(t0);
        Assertions.assertEquals(0.0, offset0.getBias(), 1.0e-15, "Perfect clock should have zero bias at reference");
        Assertions.assertEquals(0.0, offset0.getRate(), 1.0e-15, "Perfect clock should have zero rate");
        Assertions.assertEquals(0.0, offset0.getAcceleration(), 1.0e-15, "Perfect clock should have zero acceleration");

        // Test at various times before and after reference
        for (double dt : new double[]{-3600.0, -60.0, -1.0, 1.0, 60.0, 3600.0, 86400.0}) {
            ClockOffset offset = perfectClock.getOffset(t0.shiftedBy(dt));
            Assertions.assertEquals(0.0, offset.getBias(), 1.0e-15,
                    "Perfect clock bias should be zero at t0 + " + dt + "s");
            Assertions.assertEquals(0.0, offset.getRate(), 1.0e-15,
                    "Perfect clock rate should be zero at t0 + " + dt + "s");
            Assertions.assertEquals(0.0, offset.getAcceleration(), 1.0e-15,
                    "Perfect clock acceleration should be zero at t0 + " + dt + "s");
        }
    }

    @Test
    void testPerfectClockField() {
        // Test perfect clock with Field types
        final AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        final PolynomialClockModel perfectClock = new PolynomialClockModel(t0, 0.0);
        final FieldAbsoluteDate<Binary64> t064 = new FieldAbsoluteDate<>(Binary64Field.getInstance(), t0);

        FieldClockOffset<Binary64> offset = perfectClock.getFieldOffset(t064);
        Assertions.assertEquals(0.0, offset.getBias().getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, offset.getRate().getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, offset.getAcceleration().getReal(), 1.0e-15);

        // Test at shifted time
        FieldClockOffset<Binary64> offset1Day = perfectClock.getFieldOffset(t064.shiftedBy(86400.0));
        Assertions.assertEquals(0.0, offset1Day.getBias().getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, offset1Day.getRate().getReal(), 1.0e-15);
    }

    @Test
    void testConstantClock() {
        // A constant clock has a fixed bias but zero rate and acceleration
        final AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        final double constantBias = 1.23e-6; // 1.23 microseconds constant offset
        final PolynomialClockModel constantClock = new PolynomialClockModel(t0, constantBias);

        // Test at reference time
        ClockOffset offset0 = constantClock.getOffset(t0);
        Assertions.assertEquals(constantBias, offset0.getBias(), 1.0e-15,
                "Constant clock should have fixed bias at reference");
        Assertions.assertEquals(0.0, offset0.getRate(), 1.0e-15,
                "Constant clock should have zero rate");
        Assertions.assertEquals(0.0, offset0.getAcceleration(), 1.0e-15,
                "Constant clock should have zero acceleration");

        // Test that bias remains constant at different times
        for (double dt : new double[]{-86400.0, -3600.0, -1.0, 0.0, 1.0, 3600.0, 86400.0}) {
            ClockOffset offset = constantClock.getOffset(t0.shiftedBy(dt));
            Assertions.assertEquals(constantBias, offset.getBias(), 1.0e-15,
                    "Constant clock bias should remain constant at t0 + " + dt + "s");
            Assertions.assertEquals(0.0, offset.getRate(), 1.0e-15,
                    "Constant clock rate should be zero at t0 + " + dt + "s");
            Assertions.assertEquals(0.0, offset.getAcceleration(), 1.0e-15,
                    "Constant clock acceleration should be zero at t0 + " + dt + "s");
        }
    }

    @Test
    void testConstantClockField() {
        // Test constant clock with Field types
        final AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        final double constantBias = -5.67e-7; // Negative constant offset
        final PolynomialClockModel constantClock = new PolynomialClockModel(t0, constantBias);
        final FieldAbsoluteDate<Binary64> t064 = new FieldAbsoluteDate<>(Binary64Field.getInstance(), t0);

        // Test at reference time
        FieldClockOffset<Binary64> offset0 = constantClock.getFieldOffset(t064);
        Assertions.assertEquals(constantBias, offset0.getBias().getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, offset0.getRate().getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, offset0.getAcceleration().getReal(), 1.0e-15);

        // Test at different time
        FieldClockOffset<Binary64> offset1Hour = constantClock.getFieldOffset(t064.shiftedBy(3600.0));
        Assertions.assertEquals(constantBias, offset1Hour.getBias().getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, offset1Hour.getRate().getReal(), 1.0e-15);
    }

    @Test
    void testCubicClock() {
        // A cubic clock has bias, rate, acceleration, and jerk (3rd derivative)
        final AbsoluteDate t0 = AbsoluteDate.GPS_EPOCH;
        final double a0 = 1.0e-8;  // bias coefficient
        final double a1 = 2.0e-9;  // rate coefficient
        final double a2 = 3.0e-10; // acceleration coefficient
        final double a3 = 4.0e-11; // jerk coefficient
        final PolynomialClockModel cubicClock = new PolynomialClockModel(t0, a0, a1, a2, a3);

        // Test at reference time (t = 0)
        ClockOffset offset0 = cubicClock.getOffset(t0);
        Assertions.assertEquals(a0, offset0.getBias(), 1.0e-20,
                "Cubic clock bias at t0 should equal a0");
        Assertions.assertEquals(a1, offset0.getRate(), 1.0e-20,
                "Cubic clock rate at t0 should equal a1");
        Assertions.assertEquals(2.0 * a2, offset0.getAcceleration(), 1.0e-20,
                "Cubic clock acceleration at t0 should equal 2*a2");

        // Test at t = 1 second
        // bias(t) = a0 + a1*t + a2*t^2 + a3*t^3
        // rate(t) = a1 + 2*a2*t + 3*a3*t^2
        // accel(t) = 2*a2 + 6*a3*t
        double t1 = 1.0;
        ClockOffset offset1 = cubicClock.getOffset(t0.shiftedBy(t1));
        double expectedBias1 = a0 + a1 * t1 + a2 * t1 * t1 + a3 * t1 * t1 * t1;
        double expectedRate1 = a1 + 2.0 * a2 * t1 + 3.0 * a3 * t1 * t1;
        double expectedAccel1 = 2.0 * a2 + 6.0 * a3 * t1;
        Assertions.assertEquals(expectedBias1, offset1.getBias(), 1.0e-20,
                "Cubic clock bias at t=1s");
        Assertions.assertEquals(expectedRate1, offset1.getRate(), 1.0e-20,
                "Cubic clock rate at t=1s");
        Assertions.assertEquals(expectedAccel1, offset1.getAcceleration(), 1.0e-20,
                "Cubic clock acceleration at t=1s");

        // Test at t = 10 seconds
        double t10 = 10.0;
        ClockOffset offset10 = cubicClock.getOffset(t0.shiftedBy(t10));
        double expectedBias10 = a0 + a1 * t10 + a2 * t10 * t10 + a3 * t10 * t10 * t10;
        double expectedRate10 = a1 + 2.0 * a2 * t10 + 3.0 * a3 * t10 * t10;
        double expectedAccel10 = 2.0 * a2 + 6.0 * a3 * t10;
        Assertions.assertEquals(expectedBias10, offset10.getBias(), 1.0e-19,
                "Cubic clock bias at t=10s");
        Assertions.assertEquals(expectedRate10, offset10.getRate(), 1.0e-19,
                "Cubic clock rate at t=10s");
        Assertions.assertEquals(expectedAccel10, offset10.getAcceleration(), 1.0e-19,
                "Cubic clock acceleration at t=10s");

        // Test at negative time
        double tMinus5 = -5.0;
        ClockOffset offsetMinus5 = cubicClock.getOffset(t0.shiftedBy(tMinus5));
        double expectedBiasMinus5 = a0 + a1 * tMinus5 + a2 * tMinus5 * tMinus5 + a3 * tMinus5 * tMinus5 * tMinus5;
        double expectedRateMinus5 = a1 + 2.0 * a2 * tMinus5 + 3.0 * a3 * tMinus5 * tMinus5;
        double expectedAccelMinus5 = 2.0 * a2 + 6.0 * a3 * tMinus5;
        Assertions.assertEquals(expectedBiasMinus5, offsetMinus5.getBias(), 1.0e-20,
                "Cubic clock bias at t=-5s");
        Assertions.assertEquals(expectedRateMinus5, offsetMinus5.getRate(), 1.0e-20,
                "Cubic clock rate at t=-5s");
        Assertions.assertEquals(expectedAccelMinus5, offsetMinus5.getAcceleration(), 1.0e-20,
                "Cubic clock acceleration at t=-5s");
    }

    @Test
    void testCubicClockField() {
        // Test cubic clock with Field types
        final AbsoluteDate t0 = AbsoluteDate.GPS_EPOCH;
        final double a0 = 1.0e-8;
        final double a1 = 2.0e-9;
        final double a2 = 3.0e-10;
        final double a3 = 4.0e-11;
        final PolynomialClockModel cubicClock = new PolynomialClockModel(t0, a0, a1, a2, a3);
        final FieldAbsoluteDate<Binary64> t064 = new FieldAbsoluteDate<>(Binary64Field.getInstance(), t0);

        // Test at reference time
        FieldClockOffset<Binary64> offset0 = cubicClock.getFieldOffset(t064);
        Assertions.assertEquals(a0, offset0.getBias().getReal(), 1.0e-20);
        Assertions.assertEquals(a1, offset0.getRate().getReal(), 1.0e-20);
        Assertions.assertEquals(2.0 * a2, offset0.getAcceleration().getReal(), 1.0e-20);

        // Test at t = 2 seconds
        double t2 = 2.0;
        FieldClockOffset<Binary64> offset2 = cubicClock.getFieldOffset(t064.shiftedBy(t2));
        double expectedBias2 = a0 + a1 * t2 + a2 * t2 * t2 + a3 * t2 * t2 * t2;
        double expectedRate2 = a1 + 2.0 * a2 * t2 + 3.0 * a3 * t2 * t2;
        double expectedAccel2 = 2.0 * a2 + 6.0 * a3 * t2;
        Assertions.assertEquals(expectedBias2, offset2.getBias().getReal(), 1.0e-20);
        Assertions.assertEquals(expectedRate2, offset2.getRate().getReal(), 1.0e-20);
        Assertions.assertEquals(expectedAccel2, offset2.getAcceleration().getReal(), 1.0e-20);
    }

    @Test
    void testCubicClockSymmetry() {
        // Verify that cubic clock polynomial properties
        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        final double a0 = 5.0e-9;
        final double a1 = 1.0e-10;
        final double a2 = 2.0e-11;
        final double a3 = 3.0e-12;
        final PolynomialClockModel cubicClock = new PolynomialClockModel(t0, a0, a1, a2, a3);

        // Test that for symmetric times around t0, certain properties hold
        double dt = 100.0; // 100 seconds
        ClockOffset offsetPlus = cubicClock.getOffset(t0.shiftedBy(dt));
        ClockOffset offsetMinus = cubicClock.getOffset(t0.shiftedBy(-dt));

        // For cubic polynomial: f(t) = a0 + a1*t + a2*t^2 + a3*t^3
        // Even terms (a0, a2) contribute symmetrically: f(t) + f(-t) = 2*a0 + 2*a2*t^2
        // Odd terms (a1, a3) contribute anti-symmetrically: f(t) - f(-t) = 2*a1*t + 2*a3*t^3
        double biasSum = offsetPlus.getBias() + offsetMinus.getBias();
        double expectedBiasSum = 2.0 * a0 + 2.0 * a2 * dt * dt;
        Assertions.assertEquals(expectedBiasSum, biasSum, 1.0e-18,
                "Sum of biases at ±dt should equal 2*a0 + 2*a2*dt^2");

        // For the derivative: f'(t) = a1 + 2*a2*t + 3*a3*t^2
        // f'(t) + f'(-t) = 2*a1 (since even power terms cancel when t changes sign)
        // Note: We're comparing rates which are first derivatives
        double ratePlus = offsetPlus.getRate();
        double rateMinus = offsetMinus.getRate();
        double expectedRatePlus = a1 + 2.0 * a2 * dt + 3.0 * a3 * dt * dt;
        double expectedRateMinus = a1 - 2.0 * a2 * dt + 3.0 * a3 * dt * dt;
        Assertions.assertEquals(expectedRatePlus, ratePlus, 1.0e-18,
                "Rate at +dt should match polynomial derivative");
        Assertions.assertEquals(expectedRateMinus, rateMinus, 1.0e-18,
                "Rate at -dt should match polynomial derivative");

        // The sum of rates at symmetric points equals 2*a1 + 2*3*a3*dt^2
        double rateSum = ratePlus + rateMinus;
        double expectedRateSum = 2.0 * a1 + 2.0 * 3.0 * a3 * dt * dt;
        Assertions.assertEquals(expectedRateSum, rateSum, 1.0e-18,
                "Sum of rates at ±dt should equal 2*a1 + 6*a3*dt^2");
    }

    @Test
    void testAddParameterDriverBasic() {
        // Start with a simple clock with only bias
        final AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        final PolynomialClockModel clock = new PolynomialClockModel(t0);

        // Initially should have only 1 parameter (bias)
        Assertions.assertEquals(1, clock.getParametersDrivers().size(),
                "Initial clock should have only bias parameter");
        Assertions.assertTrue(clock.getParametersDrivers().getFirst().getName().contains("-clock-bias"),
                "First parameter should be bias");

        // Add a drift parameter at index 1
        final ParameterDriver drift = new ParameterDriver("-clock-drift", 0.0, 1.0,
                                                          Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                          AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        drift.setValue(1.0e-9);
        drift.setReferenceDate(t0);
        clock.addParameterDriver(1, drift);

        // Should now have 2 parameters
        Assertions.assertEquals(2, clock.getParametersDrivers().size(),
                "Clock should have 2 parameters after adding drift");
        Assertions.assertTrue(clock.getParametersDrivers().get(1).getName().contains("-clock-drift"),
                "Second parameter should be drift");
        Assertions.assertEquals(1.0e-9, clock.getParametersDrivers().get(1).getValue(t0), 1.0e-15,
                "Drift value should be preserved");
    }

    @Test
    void testAddParameterDriverWithGap() {
        // Start with only bias, then add acceleration (skipping drift)
        final AbsoluteDate t0 = AbsoluteDate.GPS_EPOCH;
        final PolynomialClockModel clock = new PolynomialClockModel(t0, 1.0e-8);

        // Initially should have only 1 parameter (bias)
        Assertions.assertEquals(1, clock.getParametersDrivers().size(),
                "Initial clock should have only bias parameter");

        // Add acceleration at index 2 (should auto-create drift at index 1)
        final ParameterDriver accel = new ParameterDriver("-clock-acceleration", 0.0, 1.0,
                                                          Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                          AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        accel.setValue(2.0e-10);
        accel.setReferenceDate(t0);
        clock.addParameterDriver(2, accel);

        // Should now have 3 parameters (bias, drift, acceleration)
        Assertions.assertEquals(3, clock.getParametersDrivers().size(),
                "Clock should have 3 parameters after adding acceleration with gap");

        // Check that drift was auto-created with zero value
        Assertions.assertTrue(clock.getParametersDrivers().get(1).getName().contains("-clock-drift"),
                "Second parameter should be drift (auto-created)");
        Assertions.assertEquals(0.0, clock.getParametersDrivers().get(1).getValue(t0), 1.0e-15,
                "Auto-created drift should have zero value");

        // Check that acceleration was added correctly
        Assertions.assertTrue(clock.getParametersDrivers().get(2).getName().contains("-clock-acceleration"),
                "Third parameter should be acceleration");
        Assertions.assertEquals(2.0e-10, clock.getParametersDrivers().get(2).getValue(t0), 1.0e-15,
                "Acceleration value should be preserved");
    }

    @Test
    void testAddParameterDriverNullValue() {
        // Test adding null driver creates empty parameter
        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        final PolynomialClockModel clock = new PolynomialClockModel(t0);

        // Add null driver at index 1
        clock.addParameterDriver(1, null);

        // Should have 2 parameters now
        Assertions.assertEquals(2, clock.getParametersDrivers().size(),
                "Clock should have 2 parameters after adding null driver");

        // Second parameter should be drift with zero value
        Assertions.assertTrue(clock.getParametersDrivers().get(1).getName().contains("-clock-drift"),
                "Added parameter should be drift");
        Assertions.assertEquals(0.0, clock.getParametersDrivers().get(1).getValue(t0), 1.0e-15,
                "Null driver should create empty parameter with zero value");
    }

    @Test
    void testAddParameterDriverMultipleGaps() {
        // Test adding a high-order term (index 4) requiring multiple intermediate terms
        final AbsoluteDate t0 = AbsoluteDate.GALILEO_EPOCH;
        final PolynomialClockModel clock = new PolynomialClockModel(t0, 1.0e-8);

        // Initially should have only 1 parameter (bias)
        Assertions.assertEquals(1, clock.getParametersDrivers().size());

        // Add term at index 4 (should auto-create indices 1, 2, 3)
        final ParameterDriver term4 = new ParameterDriver("-clock-term-4", 0.0, 1.0,
                                                          Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                          AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        term4.setValue(5.0e-12);
        term4.setReferenceDate(t0);
        clock.addParameterDriver(4, term4);

        // Should now have 5 parameters (indices 0-4)
        Assertions.assertEquals(5, clock.getParametersDrivers().size(),
                "Clock should have 5 parameters after adding term at index 4");

        // Verify all intermediate terms were created
        Assertions.assertTrue(clock.getParametersDrivers().getFirst().getName().contains("-clock-bias"));
        Assertions.assertTrue(clock.getParametersDrivers().get(1).getName().contains("-clock-drift"));
        Assertions.assertTrue(clock.getParametersDrivers().get(2).getName().contains("-clock-acceleration"));
        Assertions.assertTrue(clock.getParametersDrivers().get(3).getName().contains("-clock-term-3"));
        Assertions.assertTrue(clock.getParametersDrivers().get(4).getName().contains("-clock-term-4"));

        // Verify intermediate terms have zero values
        Assertions.assertEquals(0.0, clock.getParametersDrivers().get(1).getValue(t0), 1.0e-15);
        Assertions.assertEquals(0.0, clock.getParametersDrivers().get(2).getValue(t0), 1.0e-15);
        Assertions.assertEquals(0.0, clock.getParametersDrivers().get(3).getValue(t0), 1.0e-15);

        // Verify the added term has correct value
        Assertions.assertEquals(5.0e-12, clock.getParametersDrivers().get(4).getValue(t0), 1.0e-15);
    }

    @Test
    void testAddParameterDriverFunctionalBehavior() {
        // Test that added parameters actually affect clock behavior
        final AbsoluteDate t0 = AbsoluteDate.GPS_EPOCH;
        final PolynomialClockModel clock = new PolynomialClockModel(t0, 1.0e-8);

        // Get initial offset
        ClockOffset offset0 = clock.getOffset(t0);
        Assertions.assertEquals(1.0e-8, offset0.getBias(), 1.0e-15);
        Assertions.assertEquals(0.0, offset0.getRate(), 1.0e-15);
        Assertions.assertEquals(0.0, offset0.getAcceleration(), 1.0e-15);

        // Add drift parameter
        final ParameterDriver drift = new ParameterDriver("-clock-drift", 0.0, 1.0,
                                                          Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                          AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        drift.setValue(2.0e-9);
        drift.setReferenceDate(t0);
        clock.addParameterDriver(1, drift);

        // Clock behavior should now include drift
        ClockOffset offset1 = clock.getOffset(t0);
        Assertions.assertEquals(1.0e-8, offset1.getBias(), 1.0e-15,
                "Bias should remain unchanged");
        Assertions.assertEquals(2.0e-9, offset1.getRate(), 1.0e-15,
                "Rate should reflect added drift");

        // Test at shifted time
        ClockOffset offset2 = clock.getOffset(t0.shiftedBy(10.0));
        double expectedBias = 1.0e-8 + 2.0e-9 * 10.0;
        Assertions.assertEquals(expectedBias, offset2.getBias(), 1.0e-15,
                "Bias at t+10s should include drift contribution");

        // Add acceleration
        final ParameterDriver accel = new ParameterDriver("-clock-acceleration", 0.0, 1.0,
                                                          Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                          AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        accel.setValue(3.0e-10);
        accel.setReferenceDate(t0);
        clock.addParameterDriver(2, accel);

        // Clock behavior should now include acceleration
        ClockOffset offset3 = clock.getOffset(t0);
        Assertions.assertEquals(2.0 * 3.0e-10, offset3.getAcceleration(), 1.0e-15,
                "Acceleration should reflect added parameter");
    }

    @Test
    void testAddParameterDriverPreservesExisting() {
        // Ensure adding parameters doesn't modify existing ones
        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        final double biasValue = 5.5e-8;
        final PolynomialClockModel clock = new PolynomialClockModel(t0, biasValue);

        // Store reference to original bias driver
        ParameterDriver originalBias = clock.getParametersDrivers().getFirst();
        double originalBiasValue = originalBias.getValue(t0);

        // Add drift parameter
        final ParameterDriver drift = new ParameterDriver("-clock-drift", 0.0, 1.0,
                                                          Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                          AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        drift.setValue(1.5e-9);
        drift.setReferenceDate(t0);
        clock.addParameterDriver(1, drift);

        // Original bias should be unchanged
        Assertions.assertEquals(originalBiasValue, clock.getParametersDrivers().getFirst().getValue(t0), 1.0e-15,
                "Original bias value should not change after adding drift");
        Assertions.assertEquals(biasValue, clock.getParametersDrivers().getFirst().getValue(t0), 1.0e-15,
                "Bias should retain its original value");

        // Add acceleration with gap (should create intermediate drift if needed)
        final ParameterDriver accel = new ParameterDriver("-clock-acceleration", 0.0, 1.0,
                                                          Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                          AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        accel.setValue(2.5e-10);
        accel.setReferenceDate(t0);
        clock.addParameterDriver(2, accel);

        // All existing parameters should still have their original values
        Assertions.assertEquals(biasValue, clock.getParametersDrivers().getFirst().getValue(t0), 1.0e-15,
                "Bias should still retain its original value");
        Assertions.assertEquals(1.5e-9, clock.getParametersDrivers().get(1).getValue(t0), 1.0e-15,
                "Drift should retain its value");
    }

}
