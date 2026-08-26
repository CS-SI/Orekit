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
import org.orekit.time.FieldTimeInterpolator;
import org.orekit.utils.ImmutableFieldTimeStampedCache;

import java.util.List;

/** Offset clock model backed up by a sample.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 14.0
 */
public class SampledFieldClockModel<T extends CalculusFieldElement<T>>
    implements FieldClockModel<T> {

    /** sample. */
    private final ImmutableFieldTimeStampedCache<FieldClockOffset<T>, T> sample;

    /** Simple constructor.
     * @param sample                clock offsets sample
     * @param nbInterpolationPoints number of points to use in interpolation
     */
    public SampledFieldClockModel(final List<FieldClockOffset<T>> sample, final int nbInterpolationPoints) {
        this.sample = new ImmutableFieldTimeStampedCache<>(nbInterpolationPoints, sample);
    }

    /** Get the clock offsets cache.
     * @return clock offsets cache
     */
    public ImmutableFieldTimeStampedCache<FieldClockOffset<T>, T> getCache() {
        return sample;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getValidityStart() {
        return sample.getEarliest().getDate().toAbsoluteDate();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getValidityEnd() {
        return sample.getLatest().getDate().toAbsoluteDate();
    }

    /** {@inheritDoc} */
    @Override
    public FieldClockOffset<T> getOffset(final FieldAbsoluteDate<T> date) {
        final FieldTimeInterpolator<FieldClockOffset<T>, T> interpolator =
            new FieldClockOffsetHermiteInterpolator<>(sample.getMaxNeighborsSize());
        return interpolator.interpolate(date, sample.getNeighbors(date));
    }

    /** {@inheritDoc} */
    @Override
    public SampledClockModel toNonField() {
        return new SampledClockModel(sample.toNonField(FieldClockOffset::toNonField).getAll(),
                                     sample.getMaxNeighborsSize());
    }

}
