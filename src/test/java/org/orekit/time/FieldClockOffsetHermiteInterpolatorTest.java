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
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class FieldClockOffsetHermiteInterpolatorTest {

    @Test
    void testNoRate() {
        doTestNoRate(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestNoRate(final Field<T> field) {
        final T zero = field.getZero();
        final FieldAbsoluteDate<T> t0 = FieldAbsoluteDate.getGalileoEpoch(field);
        final FieldClockOffsetHermiteInterpolator<T> interpolator = new FieldClockOffsetHermiteInterpolator<>(4);
        FieldClockOffset<T> interpolated =
            interpolator.interpolate(t0.shiftedBy(2.5),
                                     Arrays.asList(new FieldClockOffset<>(t0.shiftedBy(0), zero.newInstance( 0.0), zero.newInstance(Double.NaN), zero.newInstance(Double.NaN)),
                                                   new FieldClockOffset<>(t0.shiftedBy(1), zero.newInstance( 1.0), zero.newInstance(Double.NaN), zero.newInstance(Double.NaN)),
                                                   new FieldClockOffset<>(t0.shiftedBy(2), zero.newInstance( 4.0), zero.newInstance(Double.NaN), zero.newInstance(Double.NaN)),
                                                   new FieldClockOffset<>(t0.shiftedBy(3), zero.newInstance( 9.0), zero.newInstance(Double.NaN), zero.newInstance(Double.NaN)),
                                                   new FieldClockOffset<>(t0.shiftedBy(4), zero.newInstance(16.0), zero.newInstance(Double.NaN), zero.newInstance(Double.NaN)),
                                                   new FieldClockOffset<>(t0.shiftedBy(5), zero.newInstance(25.0), zero.newInstance(Double.NaN), zero.newInstance(Double.NaN))));
        Assertions.assertEquals(6.25, interpolated.getOffset().getReal(),       1.0e-15);
        Assertions.assertEquals(5.00, interpolated.getRate().getReal(),         1.0e-15);
        Assertions.assertEquals(2.00, interpolated.getAcceleration().getReal(), 1.0e-15);
    }

    @Test
    void testNoAcceleration() {
        doTestNoAcceleration(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestNoAcceleration(final Field<T> field) {
        final T zero = field.getZero();
        final FieldAbsoluteDate<T> t0 = FieldAbsoluteDate.getGalileoEpoch(field);
        final FieldClockOffsetHermiteInterpolator<T> interpolator = new FieldClockOffsetHermiteInterpolator<>(4);
        FieldClockOffset<T> interpolated =
            interpolator.interpolate(t0.shiftedBy(2.5),
                                     Arrays.asList(new FieldClockOffset<>(t0.shiftedBy(0), zero.newInstance( 0.0), zero.newInstance( 0.0), zero.newInstance(Double.NaN)),
                                                   new FieldClockOffset<>(t0.shiftedBy(1), zero.newInstance( 1.0), zero.newInstance( 2.0), zero.newInstance(Double.NaN)),
                                                   new FieldClockOffset<>(t0.shiftedBy(2), zero.newInstance( 4.0), zero.newInstance( 4.0), zero.newInstance(Double.NaN)),
                                                   new FieldClockOffset<>(t0.shiftedBy(3), zero.newInstance( 9.0), zero.newInstance( 6.0), zero.newInstance(Double.NaN)),
                                                   new FieldClockOffset<>(t0.shiftedBy(4), zero.newInstance(16.0), zero.newInstance( 8.0), zero.newInstance(Double.NaN)),
                                                   new FieldClockOffset<>(t0.shiftedBy(5), zero.newInstance(25.0), zero.newInstance(10.0), zero.newInstance(Double.NaN))));
        Assertions.assertEquals(6.25, interpolated.getOffset().getReal(),       1.0e-15);
        Assertions.assertEquals(5.00, interpolated.getRate().getReal(),         1.0e-15);
        Assertions.assertEquals(2.00, interpolated.getAcceleration().getReal(), 1.0e-15);
    }

    @Test
    void testComplete() {
        doTestComplete(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestComplete(final Field<T> field) {
        final T zero = field.getZero();
        final FieldAbsoluteDate<T> t0 = FieldAbsoluteDate.getGalileoEpoch(field);
        final FieldClockOffsetHermiteInterpolator<T> interpolator = new FieldClockOffsetHermiteInterpolator<>(4);
        FieldClockOffset<T> interpolated =
            interpolator.interpolate(t0.shiftedBy(2.5),
                                     Arrays.asList(new FieldClockOffset<>(t0.shiftedBy(0), zero.newInstance( 0.0), zero.newInstance( 0.0), zero.newInstance(2.0)),
                                                   new FieldClockOffset<>(t0.shiftedBy(1), zero.newInstance( 1.0), zero.newInstance( 2.0), zero.newInstance(2.0)),
                                                   new FieldClockOffset<>(t0.shiftedBy(2), zero.newInstance( 4.0), zero.newInstance( 4.0), zero.newInstance(2.0)),
                                                   new FieldClockOffset<>(t0.shiftedBy(3), zero.newInstance( 9.0), zero.newInstance( 6.0), zero.newInstance(2.0)),
                                                   new FieldClockOffset<>(t0.shiftedBy(4), zero.newInstance(16.0), zero.newInstance( 8.0), zero.newInstance(2.0)),
                                                   new FieldClockOffset<>(t0.shiftedBy(5), zero.newInstance(25.0), zero.newInstance(10.0), zero.newInstance(2.0))));
        Assertions.assertEquals(6.25, interpolated.getOffset().getReal(),       1.0e-15);
        Assertions.assertEquals(5.00, interpolated.getRate().getReal(),         1.0e-15);
        Assertions.assertEquals(2.00, interpolated.getAcceleration().getReal(), 1.0e-15);
    }

}
