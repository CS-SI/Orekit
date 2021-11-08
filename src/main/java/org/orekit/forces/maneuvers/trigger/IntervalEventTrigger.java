/* Copyright 2002-2021 CS GROUP
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
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldAbstractDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.AbsoluteDate;

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
public abstract class IntervalEventTrigger<T extends AbstractDetector<T>> extends AbstractManeuverTriggers {

    /** Prototype intervals detector. */
    private final T prototypeDetector;

    /** Intervals detector. */
    private final T firingIntervalDetector;

    /** Cached field-based detectors. */
    private final transient Map<Field<? extends CalculusFieldElement<?>>, FieldEventDetector<? extends CalculusFieldElement<?>>> cached;

    /** Simple constructor.
     * <p>
     * Note that the {@code intervalDetector} passed as an argument is used only
     * as a <em>prototype</em> from which a new detector will be built using its
     * {@link AbstractDetector#withHandler(EventHandler) withHandler} method to
     * set up an internal handler. This implies that the {@link #getFiringIntervalDetector()}
     * will <em>not</em> return the object that was passed as an argument. However,
     * the {@link EventHandler#init(SpacecraftState, AbsoluteDate, EventDetector)
     * init}; {@link EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean)
     * eventOccurred}, and {@link EventHandler#resetState(EventDetector, SpacecraftState)
     * resetState} methods from the prototype event handler will be called too by
     * the internal handler corresponding methods. If the prototype {@link
     * EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean) eventOccurred}
     * returns {@link Action#CONTINUE}, it will be replaced by {@link Action#RESET_DERIVATIVES,
     * otherwise the prototype return will be used as is. This could be used for example to
     * set up a trigger that would generate several firing intervals and stop propagation
     * after a predefined number of maneuvers have been performed by returning {@link Action#STOP}.
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
    public IntervalEventTrigger(final T prototypeFiringIntervalDetector) {
        this.prototypeDetector      = prototypeFiringIntervalDetector;
        this.firingIntervalDetector = prototypeFiringIntervalDetector.
                                      withHandler(new TriggerHandler<>(prototypeFiringIntervalDetector,
                                                                       this::handleIntervalFiring));
        this.cached                 = new HashMap<>();
    }

    /**
     * Getter for the firing interval detector.
     * @return firing interval detector
     */
    public T getFiringIntervalDetector() {
        return firingIntervalDetector;
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
            return firingIntervalDetector.g(initialState.shiftedBy(shift)) > 0;
        } else {
            return insideThrustArcG > 0;
        }

    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventsDetectors() {
        return Stream.of(firingIntervalDetector);
    }

    /** {@inheritDoc} */
    public <S extends CalculusFieldElement<S>> Stream<FieldEventDetector<S>> getFieldEventsDetectors(final Field<S> field) {

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
     * This method is not inlined in {@link #getFieldEventsDetectors(Field)} because the
     * parameterized types confuses the Java compiler.
     * </p>
     * @param field field to which the state belongs
     * @param <D> type of the event detector
     * @param <S> type of the field elements
     * @return converted firing intervals detector
     */
    private <D extends FieldAbstractDetector<D, S>, S extends CalculusFieldElement<S>> D convertAndSetUpHandler(final Field<S> field) {
        final FieldAbstractDetector<D, S> converted = convertIntervalDetector(field, prototypeDetector);
        return converted.
               withMaxCheck(field.getZero().newInstance(prototypeDetector.getMaxCheckInterval())).
               withThreshold(field.getZero().newInstance(prototypeDetector.getThreshold())).
               withHandler(new FieldTriggerHandler<>(prototypeDetector, this::handleIntervalFiring));
    }

    /** Convert a primitive firing intervals detector into a field firing intervals detector.
     * <p>
     * There is not need to set up {@link FieldAbstractDetector#withMaxCheck(CalculusFieldElement) withMaxCheck},
     * {@link FieldAbstractDetector#withThreshold(CalculusFieldElement) withThreshold}, or
     * {@link FieldAbstractDetector#withHandler(org.orekit.propagation.events.handlers.FieldEventHandler) withHandler}
     * in the converted detector, this will be done by caller.
     * </p>
     * <p>
     * A skeleton implementation of this method to convert some {@code XyzDetector} into {@code FieldXyzDetector},
     * considering these detectors are created from a date and a number parameter is:
     * </p>
     * <pre>{@code
     *     protected <D extends FieldEventDetector<S>, S extends CalculusFieldElement<S>>
     *         FieldAbstractDetector<D, S> convertIntervalDetector(final Field<S> field, final XyzDetector detector) {
     *
     *         final FieldAbsoluteDate<S> date  = new FieldAbsoluteDate<>(field, detector.getDate());
     *         final S                    param = field.getZero().newInstance(detector.getParam());
     *
     *         @SuppressWarnings("unchecked")
     *         final FieldAbstractDetector<D, S> converted = (FieldAbstractDetector<D, S>) new FieldXyzDetector<>(date, param);
     *         return converted;
     *
     *     }
     * }</pre>
     * @param field field to which the state belongs
     * @param detector primitive firing intervals detector to convert
     * @param <D> type of the event detector
     * @param <S> type of the field elements
     * @return converted firing intervals detector
     */
    protected abstract <D extends FieldEventDetector<S>, S extends CalculusFieldElement<S>>
        FieldAbstractDetector<D, S> convertIntervalDetector(Field<S> field, T detector);

    /** Handler firing at either boundaries of the firing interval.
     * @param prototypeAction action from the prototype event handler
     * @param state SpaceCraft state to be used in the evaluation
     * @param increasing with the event occurred in an "increasing" or "decreasing" slope direction
     * @param forward indicator for forward propagation
     * @return the Action that the calling detector should pass back to the evaluation system
     */
    private Action handleIntervalFiring(final Action prototypeAction, final SpacecraftState state,
                                        final boolean increasing, final boolean forward) {
        final Action action = prototypeAction == Action.CONTINUE ? Action.RESET_DERIVATIVES : prototypeAction;
        if (forward) {
            getFirings().addValidAfter(increasing, state.getDate());
        } else {
            getFirings().addValidBefore(!increasing, state.getDate());
        }
        return action;
    }

    /** Handler firing at either boundaries of the firing interval.
     * @param prototypeAction action from the prototype event handler
     * @param state SpaceCraft state to be used in the evaluation
     * @param increasing with the event occurred in an "increasing" or "decreasing" slope direction
     * @param forward indicator for forward propagation
     * @param <S> type of the field elements
     * @return the Action that the calling detector should pass back to the evaluation system
     */
    private <S extends CalculusFieldElement<S>> Action handleIntervalFiring(final Action prototypeAction, final FieldSpacecraftState<S> state,
                                                                            final boolean increasing, final boolean forward) {
        final Action action = prototypeAction == Action.CONTINUE ? Action.RESET_DERIVATIVES : prototypeAction;
        if (forward) {
            getFirings().addValidAfter(increasing, state.getDate().toAbsoluteDate());
        } else {
            getFirings().addValidBefore(!increasing, state.getDate().toAbsoluteDate());
        }
        return action;
    }

}
