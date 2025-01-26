/* Copyright 2002-2025 CS GROUP
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
package org.orekit.estimation.measurements.generation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;


/** Interface for generating individual {@link ObservedMeasurement measurements}.
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 9.3
 */
public interface MeasurementBuilder<T extends ObservedMeasurement<T>> {

    /** Initialize builder at the start of a measurements generation.
     * <p>
     * This method is called once at the start of the measurements generation. It
     * may be used by the builder to initialize some internal data
     * if needed, typically setting up parameters reference dates.
     * </p>
     * @param start start of the measurements time span
     * @param end end of the measurements time span
     */
    void init(AbsoluteDate start, AbsoluteDate end);

    /** Add a modifier.
     * @param modifier modifier to add
     */
    void addModifier(EstimationModifier<T> modifier);

    /** Get the modifiers that apply to a measurement.
     * @return modifiers that apply to a measurement
     * @see #addModifier(EstimationModifier)
     */
    List<EstimationModifier<T>> getModifiers();

    /** Get the satellites related to this measurement.
     * @return satellites related to this measurement
     * @since 12.0
     */
    ObservableSatellite[] getSatellites();

    /** Generate a single measurement.
     * @param date measurement date
     * @param interpolators interpolators relevant for this builder
     * @return generated measurement
     * @since 13.0
     */
    EstimatedMeasurementBase<T> build(AbsoluteDate date, Map<ObservableSatellite, OrekitStepInterpolator> interpolators);

    /** Generate a single measurement.<p>
     *
     * Warning: This method uses "shiftedBy" so it is not as accurate as the method above that uses interpolators.
     *
     * @param date measurement date
     * @param states all spacecraft states (i.e. including ones that may not be relevant for the current builder)
     * @return generated measurement
     * @since 12.1
     */
    default EstimatedMeasurementBase<T> build(AbsoluteDate date, SpacecraftState[] states) {
        final Map<ObservableSatellite, OrekitStepInterpolator> interpolators = new ConcurrentHashMap<>();

        for (int i = 0; i < states.length; i++) {
            final ObservableSatellite sat = getSatellites()[i];
            final SpacecraftState state = states[i];

            final OrekitStepInterpolator interpolator = new OrekitStepInterpolator() {
                /** {@inheritDoc} */
                @Override
                public OrekitStepInterpolator restrictStep(final SpacecraftState newPreviousState, final SpacecraftState newCurrentState) {
                    return null;
                }
                /** {@inheritDoc} */
                @Override
                public boolean isPreviousStateInterpolated() {
                    return false;
                }
                /** {@inheritDoc} */
                @Override
                public boolean isForward() {
                    return true;
                }
                /** {@inheritDoc} */
                @Override
                public boolean isCurrentStateInterpolated() {
                    return false;
                }
                /** {@inheritDoc} */
                @Override
                public SpacecraftState getPreviousState() {
                    return state;
                }
                /** {@inheritDoc} */
                @Override
                public SpacecraftState getInterpolatedState(final AbsoluteDate date) {
                    return state.shiftedBy(date.durationFrom(state));
                }
                /** {@inheritDoc} */
                @Override
                public SpacecraftState getCurrentState() {
                    return state;
                }
            };
            interpolators.put(sat, interpolator);
        }

        return build( date, interpolators);
    }
}
