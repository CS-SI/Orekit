/* Copyright 2002-2026 CS GROUP
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
package org.orekit.propagation.events;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.functions.EventFunction;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.intervals.FieldAdaptableInterval;
import org.orekit.time.FieldAbsoluteDate;

/** This interface represents space-dynamics aware events detectors.
 *
 * <p>It mirrors the {@link org.hipparchus.ode.events.FieldODEEventHandler
 * FieldODEEventHandler} interface from <a href="https://hipparchus.org/">
 * Hipparchus</a> but provides a space-dynamics interface to the
 * methods.</p>
 *
 * <p>Events detectors are a useful solution to meet the requirements
 * of propagators concerning discrete conditions. The state of each
 * event detector is queried by the propagator from time to time, at least
 * once every {@link #getMaxCheckInterval() max check interval} but it may
 * be more frequent. When the sign of the underlying g switching function
 * changes, a root-finding algorithm is run to precisely locate the event,
 * down to a configured {@link #getThreshold() convergence threshold}. The
 * {@link #getMaxCheckInterval() max check interval} is therefore devoted to
 * separate roots and is often much larger than the  {@link #getThreshold()
 * convergence threshold}.</p>
 *
 * <p>The physical meaning of the g switching function is not really used
 * by the event detection algorithms. Its varies from event detector to
 * event detector. One example would be a visibility detector that could use the
 * angular elevation of the satellite above horizon as a g switching function.
 * In this case, the function would switch from negative to positive when the
 * satellite raises above horizon and it would switch from positive to negative
 * when it sets backs below horizon. Another example would be an apside detector
 * that could use the dot product of position and velocity. In this case, the
 * function would switch from negative to positive when the satellite crosses
 * periapsis and it would switch from positive to negative when the satellite
 * crosses apoapsis.</p>
 *
 * <p>When the precise state at which the g switching function changes has been
 * located, the corresponding event is triggered, by calling the {@link
 * FieldEventHandler#eventOccurred(FieldSpacecraftState, FieldEventDetector, boolean)
 * eventOccurred} method from the associated {@link #getHandler() handler}.
 * The method can do whatever it needs with the event (logging it, performing
 * some processing, ignore it ...). The return value of the method will be used by
 * the propagator to stop or resume propagation, possibly changing the state vector.</p>
 *
 * @param <T> type of the field element
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 * @see EventDetector
 */
public interface FieldEventDetector <T extends CalculusFieldElement<T>> {

    /**
     * Builds instance from event function, handler and detection settings.
     * @param eventFunction event function
     * @param eventHandler handler
     * @param eventDetectionSettings detection settings
     * @param <S> field type
     * @return event detector
     * @since 14.0
     */
    static <S extends CalculusFieldElement<S>> FieldEventDetector<S> of(final EventFunction eventFunction,
                                                                        final FieldEventHandler<S> eventHandler,
                                                                        final FieldEventDetectionSettings<S> eventDetectionSettings) {
        return new FieldEventDetector<S>() {

            @Override
            public EventFunction getEventFunction() {
                return eventFunction;
            }

            @Override
            public S g(final FieldSpacecraftState<S> s) {
                return getEventFunction().value(s);
            }

            @Override
            public FieldEventHandler<S> getHandler() {
                return eventHandler;
            }

            @Override
            public FieldEventDetectionSettings<S> getDetectionSettings() {
                return eventDetectionSettings;
            }
        };
    }

    /**
     * Builds instance from non-Field event detector.
     * It might not perform as expected if the latter has a specialized handler.
     * @param field field
     * @param eventHandler handler
     * @param detector non-Field detector
     * @param <S> field type
     * @return detector
     * @since 14.0
     */
    static <S extends CalculusFieldElement<S>> FieldEventDetector<S> of(final Field<S> field,
                                                                        final FieldEventHandler<S> eventHandler,
                                                                        final EventDetector detector) {
        final FieldEventDetectionSettings<S> fieldEventDetectionSettings = new FieldEventDetectionSettings<>(field,
                detector.getDetectionSettings());

        return new FieldEventDetector<S>() {

            @Override
            public void init(final FieldSpacecraftState<S> s0, final FieldAbsoluteDate<S> t) {
                FieldEventDetector.super.init(s0, t);
                detector.init(s0.toSpacecraftState(), t.toAbsoluteDate());
            }

            @Override
            public void reset(final FieldSpacecraftState<S> state, final FieldAbsoluteDate<S> target) {
                FieldEventDetector.super.reset(state, target);
                detector.reset(state.toSpacecraftState(), target.toAbsoluteDate());
            }

            @Override
            public void finish(final FieldSpacecraftState<S> state) {
                FieldEventDetector.super.finish(state);
                detector.finish(state.toSpacecraftState());
            }

            @Override
            public EventFunction getEventFunction() {
                return detector.getEventFunction();
            }

            @Override
            public S g(final FieldSpacecraftState<S> s) {
                return getEventFunction().value(s);
            }

            @Override
            public FieldEventHandler<S> getHandler() {
                return eventHandler;
            }

            @Override
            public FieldEventDetectionSettings<S> getDetectionSettings() {
                return fieldEventDetectionSettings;
            }
        };
    }
    /** Initialize event detector at the start of a propagation.
     * <p>
     * This method is called once at the start of the propagation. It
     * may be used by the event detector to initialize some internal data
     * if needed.
     * </p>
     * <p>
     * The default implementation initializes the handler.
     * </p>
     * @param s0 initial state
     * @param t target time for the integration
     *
     */
    default void init(final FieldSpacecraftState<T> s0, final FieldAbsoluteDate<T> t) {
        getHandler().init(s0, t, this);
    }

    /** Reset the event detector during propagation when the state is modified by an event or an additional data provider.
     * <p>
     * The default implementation does nothing.
     * </p>
     * @param state current state
     * @param target target time for the integration
     * @since 13.0
     */
    default void reset(final FieldSpacecraftState<T> state, final FieldAbsoluteDate<T> target) {
        // nothing by default
    }

    /** Compute the value of the switching function.
     * This function must be continuous (at least in its roots neighborhood),
     * as the integrator will need to find its roots to locate the events.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     */
    T g(FieldSpacecraftState<T> s);

    /**
     * Get the event function. It defines g both for double and Field.
     * @return event function
     * @since 14.0
     */
    default EventFunction getEventFunction() {
        return EventFunction.of(getThreshold().getField(), this::g);
    }

    /** Get the convergence threshold in the event time search.
     * @return convergence threshold (s)
     */
    default T getThreshold() {
        return getDetectionSettings().getThreshold();
    }

    /** Get maximal time interval between switching function checks.
     * @return maximal time interval (s) between switching function checks
     */
    default FieldAdaptableInterval<T> getMaxCheckInterval() {
        return getDetectionSettings().getMaxCheckInterval();
    }

    /** Get maximal number of iterations in the event time search.
     * @return maximal number of iterations in the event time search
     */
    default int getMaxIterationCount() {
        return getDetectionSettings().getMaxIterationCount();
    }

    /** Get the handler.
     * @return event handler to call at event occurrences
     * @since 12.0
     */
    FieldEventHandler<T> getHandler();

    /**
     * This method finalizes the event detector's job.
     * @param state state at propagation end
     * @since 12.2
     */
    default void finish(final FieldSpacecraftState<T> state) {
        getHandler().finish(state, this);
    }

    /**
     * Getter for the settings.
     * @return detection settings
     * @since 12.2
     */
    FieldEventDetectionSettings<T> getDetectionSettings();

}
