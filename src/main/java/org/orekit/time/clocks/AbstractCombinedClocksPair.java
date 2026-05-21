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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeUtils;

/** Clock model combining two underlying models.
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class AbstractCombinedClocksPair implements ClockModel {

    /** First underlying clock. */
    private final ClockModel clock1;

    /** Second underlying clock. */
    private final ClockModel clock2;

    /** Simple constructor.
     * @param clock1 first underlying clock
     * @param clock2 second underlying clock
     */
    protected  AbstractCombinedClocksPair(final ClockModel clock1, final ClockModel clock2) {
        this.clock1 = clock1;
        this.clock2 = clock2;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getValidityStart() {
        return TimeUtils.latest(clock1.getValidityStart(), clock2.getValidityStart());
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getValidityEnd() {
        return TimeUtils.earliest(clock1.getValidityEnd(), clock2.getValidityEnd());
    }

    /** {@inheritDoc} */
    @Override
    public ClockOffset getOffset(final AbsoluteDate date) {
        return combine(clock1.getOffset(date), clock2.getOffset(date));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldClockOffset<T> getOffset(final FieldAbsoluteDate<T> date) {
        return combine(clock1.getOffset(date), clock2.getOffset(date));
    }

    /** Combine two offsets.
     * @param offset1 first offset
     * @param offset2 second offset
     * @return combined offset
     */
    protected abstract ClockOffset combine(ClockOffset offset1, ClockOffset offset2);

    /** Combine two offsets.
     * @param <T> type of the field elements
     * @param offset1 first offset
     * @param offset2 second offset
     * @return combined offset
     */
    protected abstract <T extends CalculusFieldElement<T>> FieldClockOffset<T> combine(FieldClockOffset<T> offset1,
                                                                                       FieldClockOffset<T> offset2);

}
