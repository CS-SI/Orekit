/* Copyright 2002-2023 CS GROUP
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
import org.hipparchus.analysis.interpolation.FieldHermiteInterpolator;
import org.hipparchus.util.MathArrays;

import java.util.List;

/**
 * Hermite interpolator of time stamped field value.
 * <p>
 * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation points
 * (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a>
 * and numerical problems (including NaN appearing).
 *
 * @author Vincent Cucchietti
 * @see FieldHermiteInterpolator
 * @see FieldTimeInterpolator
 * @param <KK> type of the field elements
 */
public class TimeStampedFieldHermiteInterpolator<KK extends CalculusFieldElement<KK>>
        extends AbstractFieldTimeInterpolator<TimeStampedField<KK>, KK> {

    /**
     * Constructor with :
     * <ul>
     *     <li>Default number of interpolation points of {@code DEFAULT_INTERPOLATION_POINTS}</li>
     *     <li>Default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s)</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     */
    public TimeStampedFieldHermiteInterpolator() {
        this(DEFAULT_INTERPOLATION_POINTS);
    }

    /**
     * Constructor with :
     * <ul>
     *     <li>Default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s)</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     */
    public TimeStampedFieldHermiteInterpolator(final int interpolationPoints) {
        this(interpolationPoints, DEFAULT_EXTRAPOLATION_THRESHOLD_SEC);
    }

    /**
     * Constructor.
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     * @param extrapolationThreshold extrapolation threshold beyond which the propagation will fail
     */
    public TimeStampedFieldHermiteInterpolator(final int interpolationPoints, final double extrapolationThreshold) {
        super(interpolationPoints, extrapolationThreshold);
    }

    /**
     * {@inheritDoc}
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small samples (about 10-20 points)
     * in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a> and numerical
     * problems (including NaN appearing).
     */
    @Override
    protected TimeStampedField<KK> interpolate(final InterpolationData interpolationData) {
        final FieldHermiteInterpolator<KK> interpolator = new FieldHermiteInterpolator<>();

        // Fill interpolator with sample
        final Field<KK>                  field             = interpolationData.getField();
        final KK                         zero              = interpolationData.getZero();
        final FieldAbsoluteDate<KK>      interpolationDate = interpolationData.getInterpolationDate();
        final List<TimeStampedField<KK>> neighborList      = interpolationData.getNeighborList();
        for (TimeStampedField<KK> value : neighborList) {
            final KK   deltaT    = value.getDate().durationFrom(interpolationDate);
            final KK[] tempArray = MathArrays.buildArray(field, 1);
            tempArray[0] = value.getValue();
            interpolator.addSamplePoint(deltaT, tempArray);
        }

        return new TimeStampedField<>(interpolator.value(zero)[0], interpolationDate);
    }
}
