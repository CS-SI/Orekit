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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

/**
 * Clock model for a clock with constant offset.
 *
 * @author Luc Maisonobe
 * @since 14.0
 */
public class ConstantClockModel implements ClockModel {
    /** Constant offset. */
    private final ParameterDriver offset;

    /**
     * Simple constructor.
     *
     * @param offset
     *               constant offset
     */
    public ConstantClockModel(final double offset) {
        this.offset = new ParameterDriver("a0", 0.0, FastMath.scalb(1.0, -10),
                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.offset.setValue(offset);
    }


    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getValidityStart() {
        return AbsoluteDate.PAST_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getValidityEnd() {
        return AbsoluteDate.FUTURE_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Arrays.asList(offset);
    }

    /** {@inheritDoc} */
    @Override
    public ClockOffset getOffset(final AbsoluteDate date) {
        return new ClockOffset(date, offset.getValue(date), 0, 0);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldClockOffset<T> getFieldOffset(final FieldAbsoluteDate<T> date) {
        final AbsoluteDate aDate = date.toAbsoluteDate();
        final T zero = date.getField().getZero();
        return new FieldClockOffset<>(date, zero.newInstance(offset.getValue(aDate)), zero, zero);
    }

    /** {@inheritDoc} */
    @Override
    public FieldClockModel<Gradient> getFieldModel(final int freeParameters,
        final Map<String, Integer> indices, final AbsoluteDate date) {
        return new ConstantFieldClockModel<>(null, Gradient.constant(freeParameters, offset.getValue(date)));
    }
}
