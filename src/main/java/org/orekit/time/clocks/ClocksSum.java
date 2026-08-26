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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.Gradient;

import java.util.Map;
import java.util.function.DoubleFunction;

/**
 * Clock model computing the sum of two underlying models.
 *
 * @author Luc Maisonobe
 * @since 14.0
 */
public class ClocksSum extends AbstractCombinedClocksPair {

    /**
     * Simple constructor.
     * <p>
     * The combined clock is {@code clock1 + clock2}
     * </p>
     *
     * @param clock1 first underlying clock
     * @param clock2 second underlying clock
     */
    public ClocksSum(final ClockModel clock1, final ClockModel clock2) {
        super(clock1, clock2);
    }

    @Override
    protected ClockOffset combine(final ClockOffset offset1, final ClockOffset offset2) {
        return offset1.add(offset2);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldClocksSum<T> toField(final DoubleFunction<T> converter) {
        return new FieldClocksSum<>(getClock1().toField(converter), getClock2().toField(converter));
    }

    /** {@inheritDoc} */
    @Override
    public FieldClocksSum<Gradient> toGradient(final int freeParameters, final Map<String, Integer> indices) {
        return new FieldClocksSum<>(getClock1().toGradient(freeParameters, indices),
                                    getClock2().toGradient(freeParameters, indices));
    }

}
