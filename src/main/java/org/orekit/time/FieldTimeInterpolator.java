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
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This interface represents objects that can interpolate a time stamped value with respect to time.
 *
 * @param <T> type of the interpolated instance
 * @param <KK> type of the field element
 *
 * @author Vincent Cucchietti
 * @see FieldAbsoluteDate
 * @see FieldTimeStamped
 * @see CalculusFieldElement
 */
public interface FieldTimeInterpolator<T extends FieldTimeStamped<KK>, KK extends CalculusFieldElement<KK>> {

    /**
     * Get an interpolated instance.
     *
     * @param interpolationDate interpolation date
     * @param sample time stamped sample
     *
     * @return a new instance, interpolated at specified date
     *
     * @see TimeStamped
     * @see AbsoluteDate
     */
    default T interpolate(AbsoluteDate interpolationDate, Stream<T> sample) {
        return interpolate(interpolationDate, sample.collect(Collectors.toList()));
    }

    /**
     * Get an interpolated instance.
     *
     * @param interpolationDate interpolation date
     * @param sample time stamped sample
     *
     * @return a new instance, interpolated at specified date
     */
    default T interpolate(AbsoluteDate interpolationDate, Collection<T> sample) {
        final Optional<T> optionalElement = sample.stream().findAny();
        if (optionalElement.isPresent()) {
            final T element = optionalElement.get();
            return interpolate(new FieldAbsoluteDate<>(element.getDate().getField(), interpolationDate), sample);
        }
        throw new OrekitIllegalArgumentException(OrekitMessages.NOT_ENOUGH_DATA, 0);
    }

    /**
     * Get an interpolated instance.
     *
     * @param interpolationDate interpolation date
     * @param sample time stamped sample
     *
     * @return a new instance, interpolated at specified date
     *
     * @see TimeStamped
     * @see AbsoluteDate
     */
    T interpolate(FieldAbsoluteDate<KK> interpolationDate, Stream<T> sample);

    /**
     * Get an interpolated instance.
     *
     * @param interpolationDate interpolation date
     * @param sample time stamped sample
     *
     * @return a new instance, interpolated at specified date
     */
    T interpolate(FieldAbsoluteDate<KK> interpolationDate, Collection<T> sample);

    /**
     * Get all lowest level interpolators implemented by this instance, otherwise return a list with this instance only.
     * <p>
     * An example would be the spacecraft state interpolator which can use different interpolators for each of its attributes
     * (orbit, absolute position-velocity-acceleration coordinates, mass...). In this case, it would return the list of all
     * of these interpolators (or possibly all of their sub-interpolators if they were to use multiple interpolators
     * themselves).
     *
     * @return list of interpolators
     */
    List<FieldTimeInterpolator<? extends FieldTimeStamped<KK>, KK>> getSubInterpolators();

    /**
     * Get the number of interpolation points. In the specific case where this interpolator contains multiple
     * sub-interpolators, this method will return the maximum number of interpolation points required among all
     * sub-interpolators.
     *
     * @return the number of interpolation points
     *
     * @since 12.0.1
     */
    int getNbInterpolationPoints();

    /** Get the extrapolation threshold.
     * @return get the extrapolation threshold.
     */
    double getExtrapolationThreshold();
}
