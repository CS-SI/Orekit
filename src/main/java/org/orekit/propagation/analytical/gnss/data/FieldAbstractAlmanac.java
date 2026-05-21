/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.propagation.analytical.gnss.data;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.frames.Frame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.gnss.FieldGnssPropagator;

import java.util.function.Function;

/**
 * Base class for GNSS almanacs.
 * @param <T> type of the field elements
 * @param <O> type of the orbital elements (non-field version)
 * @author Luc Maisonobe
 * @since 13.0
 */
public abstract class FieldAbstractAlmanac<T extends CalculusFieldElement<T>,
                                           O extends AbstractAlmanac<O>>
    extends FieldCommonGnssData<T, O> {

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    protected FieldAbstractAlmanac(final Field<T> field, final O original) {
        super(field, original);
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param original regular non-field instance
     * @param converter for field elements
     */
    protected <V extends CalculusFieldElement<V>> FieldAbstractAlmanac(final Function<V, T> converter,
                                                                       final FieldAbstractAlmanac<V, O> original) {
        super(converter, original);
    }

    /**
     * Get the propagator corresponding to the navigation message.
     * <p>
     * The attitude provider is set by default to be aligned with the provided inertial frame.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.
     * </p>
     * @param inertial inertial frame, use to provide the propagated orbit
     * @param bodyFixed body fixed frame, corresponding to the navigation message
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator(AttitudeProvider, Frame, Frame, CalculusFieldElement)
     * @since 14.0
     */
    public FieldGnssPropagator<T> getPropagator(final Frame inertial, final Frame bodyFixed) {
        return getPropagator(new FrameAlignedProvider(inertial),
                             inertial, bodyFixed,
                             getMu().newInstance(Propagator.DEFAULT_MASS));
    }

    /**
     * Get the propagator corresponding to the navigation message.
     * @param provider attitude provider
     * @param inertial inertial frame, use to provide the propagated orbit
     * @param bodyFixed body fixed frame, corresponding to the navigation message
     * @param mass spacecraft mass in kg
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator(Frame, Frame)
     * @since 14.0
     */
    public FieldGnssPropagator<T> getPropagator(final AttitudeProvider provider,
                                                final Frame inertial, final Frame bodyFixed, final T mass) {
        return new FieldGnssPropagator<>(this, inertial, bodyFixed, provider, mass);
    }

}
