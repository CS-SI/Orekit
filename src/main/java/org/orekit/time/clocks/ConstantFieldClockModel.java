/* Copyright 2025-2026 Hawkeye 360 (HE360)
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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Field Clock model for a clock with constant offset.
 * @param <T> type of the field elements
 * @author Brian Carter
 * @since 14.0
 * @see ConstantClockModel
 */
public class ConstantFieldClockModel<T extends CalculusFieldElement<T>> implements FieldClockModel<T> {

    /** Constant offset. */
    private final T offset;

    /** Simple constructor.
     * @param offset offset value
     */
    public ConstantFieldClockModel(final T offset) {
        this.offset = offset;
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
    public FieldClockOffset<T> getOffset(final FieldAbsoluteDate<T> date) {
        final T zero = date.getField().getZero();
        return new FieldClockOffset<>(date, offset, zero, zero);
    }

    /** {@inheritDoc} */
    @Override
    public ClockModel toNonField() {
        return new ConstantClockModel(offset.getReal());
    }

}
