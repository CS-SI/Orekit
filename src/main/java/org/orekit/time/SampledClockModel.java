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
import org.orekit.utils.ImmutableTimeStampedCache;

import java.util.List;
import java.util.stream.Stream;

/** Offset clock model backed up by a sample.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class SampledClockModel implements ClockModel {

    /** sample. */
    private final ImmutableTimeStampedCache<ClockOffset> sample;

    /** Simple constructor.
     * @param sample clock offsets sample
     * @param nbInterpolationPoints number of points to use in interpolation
     */
    public SampledClockModel(final List<ClockOffset> sample, final int nbInterpolationPoints) {
        this.sample = new ImmutableTimeStampedCache<>(nbInterpolationPoints, sample);
    }

    /** Get the clock offsets cache.
     * @return clock offsets cache
     */
    public ImmutableTimeStampedCache<ClockOffset> getCache() {
        return sample;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getValidityStart() {
        return sample.getEarliest().getDate();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getValidityEnd() {
        return sample.getLatest().getDate();
    }

    /** {@inheritDoc} */
    @Override
    public ClockOffset getOffset(final AbsoluteDate date) {
        return new ClockOffsetHermiteInterpolator(sample.getMaxNeighborsSize()).
            interpolate(date, sample.getNeighbors(date));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldClockOffset<T> getOffset(final FieldAbsoluteDate<T> date) {

        // convert the neighbors to field
        final Field<T> field = date.getField();
        final T        zero  = field.getZero();
        final Stream<FieldClockOffset<T>> fieldSample =
            sample.
                getNeighbors(date.toAbsoluteDate()).
                map(c -> {
                    final FieldAbsoluteDate<T> dateF   = new FieldAbsoluteDate<>(field, c.getDate());
                    final T                    offsetF = zero.newInstance(c.getOffset());
                    final T rateF;
                    final T accelerationF;
                    if (Double.isNaN(c.getRate())) {
                        // no rate available
                        rateF         = null;
                        accelerationF = null;
                    } else {
                        // rate available
                        rateF = zero.newInstance(c.getRate());
                        accelerationF = Double.isNaN(c.getAcceleration()) ?
                                        null :
                                        zero.newInstance(c.getAcceleration());
                    }
                    return new FieldClockOffset<>(dateF, offsetF, rateF, accelerationF);
                });

        // perform interpolation
        final FieldClockOffsetHermiteInterpolator<T> interpolator =
            new FieldClockOffsetHermiteInterpolator<>(sample.getMaxNeighborsSize());
        return interpolator.interpolate(date, fieldSample);

    }

}
