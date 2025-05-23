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
package org.orekit.forces.maneuvers.trigger;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.events.Action;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Maneuver triggers based on a single event detector that defines firing intervals.
 * <p>
 * Firing intervals correspond to time spans with positive value of the event detector
 * {@link EventDetector#g(SpacecraftState) g} function.
 * </p>
 * @param <T> type of the interval detector
 * @see StartStopEventsTrigger
 * @author Luc Maisonobe
 * @since 11.1
 */
public abstract class IntervalEventTrigger<T extends EventDetector> extends AbstractManeuverTriggers {

    /** Intervals detector. */
    private final ManeuverTriggerDetector<T> firingIntervalDetector;

    /** Cached field-based detectors. */
    private final transient Map<Field<? extends CalculusFieldElement<?>>, FieldEventDetector<? extends CalculusFieldElement<?>>> cached;

    /** Simple constructor.
     * <p>
     * Note that the {@code intervalDetector} passed as an argument is used only
     * as a <em>prototype</em> from which a new detector will be built using
     * {@link ManeuverTriggerDetector}. The original event handler from the prototype
     * will be <em>ignored</em> and never called.
     * </p>
     * <p>
     * If the trigger is used in a {@link org.orekit.propagation.FieldPropagator field-based propagation},
     * the detector will be automatically converted to a field equivalent. Beware however that the
     * {@link FieldEventHandler#eventOccurred(FieldSpacecraftState, FieldEventDetector, boolean) eventOccurred}
     * of the converted propagator <em>will</em> call the method with the same name in the prototype
     * detector, in order to get the correct return value.
     * </p>
     * @param prototypeFiringIntervalDetector prototype detector for firing interval
     */
    protected IntervalEventTrigger(final T prototypeFiringIntervalDetector) {
        this.firingIntervalDetector = new ManeuverTriggerDetector<>(prototypeFiringIntervalDetector, new Handler());
        this.cached                 = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        this.firingIntervalDetector.init(initialState, target);
        super.init(initialState, target);
    }

    /** {@inheritDoc} */
    @Override
    public <D extends CalculusFieldElement<D>> void init(final FieldSpacecraftState<D> initialState,
                                                         final FieldAbsoluteDate<D> target) {
        this.firingIntervalDetector.init(initialState.toSpacecraftState(), target.toAbsoluteDate());
        super.init(initialState, target);
    }

    /**
     * Getter for the firing interval detector.
     * @return firing interval detector
     */
    public T getFiringIntervalDetector() {
        return firingIntervalDetector.getDetector();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean isFiringOnInitialState(final SpacecraftState initialState, final boolean isForward) {

        // set the initial value of firing
        final double insideThrustArcG = firingIntervalDetector.g(initialState);
        if (insideThrustArcG == 0) {
            // bound of arc
            // check state for the upcoming times
            final double shift = (isForward ? 2 : -2) * firingIntervalDetector.getThreshold();
            if (firingIntervalDetector.g(initialState.shiftedBy(shift)) > 0) {
                // we are entering the firing interval, from start if forward, from end if backward
                notifyResetters(initialState, isForward);
                return true;
            } else {
                // we are leaving the firing interval, from end if forward, from start if backward
                notifyResetters(initialState, !isForward);
                return false;
            }
        } else {
            return insideThrustArcG > 0;
        }

    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        return Stream.of(firingIntervalDetector);
    }

    /** {@inheritDoc} */
    @Override
    public <S extends CalculusFieldElement<S>> Stream<FieldEventDetector<S>> getFieldEventDetectors(final Field<S> field) {

        @SuppressWarnings("unchecked")
        FieldEventDetector<S> fd = (FieldEventDetector<S>) cached.get(field);
        if (fd == null) {
            fd = convertAndSetUpHandler(field);
            cached.put(field, fd);
        }

        return Stream.of(fd);

    }

    /** Convert a detector and set up check interval, threshold and new handler.
     * <p>
     * This method is not inlined in {@link #getFieldEventDetectors(Field)} because the
     * parameterized types confuses the Java compiler.
     * </p>
     * @param field field to which the state belongs
     * @param <D> type of the event detector
     * @param <S> type of the field elements
     * @return converted firing intervals detector
     */
    private <D extends FieldEventDetector<S>, S extends CalculusFieldElement<S>> FieldManeuverTriggerDetector<S, D> convertAndSetUpHandler(final Field<S> field) {
        final D converted = convertIntervalDetector(field, firingIntervalDetector.getDetector());
        return new FieldManeuverTriggerDetector<>(converted, new FieldHandler<>());
    }

    /** Convert a primitive firing intervals detector into a field firing intervals detector.
     * <p>
     * The {@link org.orekit.propagation.events.FieldEventDetectionSettings} must be set up in conformance with the
     * non-field detector.
     * </p>
     * <p>
     * A skeleton implementation of this method to convert some {@code XyzDetector} into {@code FieldXyzDetector},
     * considering these detectors are created from a date and a number parameter is:
     * </p>
     * <pre>{@code
     *     protected <D extends FieldEventDetector<S>, S extends CalculusFieldElement<S>>
     *         D convertIntervalDetector(final Field<S> field, final XyzDetector detector) {
     *
     *         final FieldAbsoluteDate<S> date  = new FieldAbsoluteDate<>(field, detector.getDate());
     *         final S                    param = field.getZero().newInstance(detector.getParam());
     *
     *         D converted = (D) new FieldXyzDetector<>(date, param).withDetectionSettings(field, detector.getDetectionSettings());
     *         return converted;
     *
     *     }
     * }
     * </pre>
     * @param field field to which the state belongs
     * @param detector primitive firing intervals detector to convert
     * @param <D> type of the event detector
     * @param <S> type of the field elements
     * @return converted firing intervals detector
     */
    protected abstract <D extends FieldEventDetector<S>, S extends CalculusFieldElement<S>>
        D convertIntervalDetector(Field<S> field, T detector);

    /** Local handler for both start and stop triggers. */
    private class Handler extends TriggerHandler {

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final SpacecraftState s, final EventDetector detector, final boolean increasing) {
            if (isForward()) {
                getFirings().addValidAfter(increasing, s.getDate(), false);
            } else {
                getFirings().addValidBefore(!increasing, s.getDate(), false);
            }
            notifyResetters(s, increasing);
            return determineAction(detector, s);
        }

    }

    /** Local handler for both start and stop triggers.
     * @param <S> type of the field elements
     */
    private class FieldHandler<S extends CalculusFieldElement<S>> extends FieldTriggerHandler<S> {

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final FieldSpacecraftState<S> s, final FieldEventDetector<S> detector, final boolean increasing) {
            if (isForward()) {
                getFirings().addValidAfter(increasing, s.getDate().toAbsoluteDate(), false);
            } else {
                getFirings().addValidBefore(!increasing, s.getDate().toAbsoluteDate(), false);
            }
            notifyResetters(s, increasing);
            return determineAction(detector, s);
        }
    }

}
