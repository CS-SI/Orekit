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
 * Maneuver triggers based on a pair of event detectors that defines firing start and stop.
 * <p>
 * The thruster starts firing when the start detector becomes
 * positive. The thruster stops firing when the stop detector becomes positive.
 * The 2 detectors should not be positive at the same time. A date detector is
 * not suited as it does not delimit an interval. They can be both negative at
 * the same time.
 * </p>
 * @param <A> type of the start detector
 * @param <O> type of the stop detector
 * @see IntervalEventTrigger
 * @author Luc Maisonobe
 * @since 11.1
 */
public abstract class StartStopEventsTrigger<A extends AbstractDetector<A>, O extends AbstractDetector<O>> extends AbstractManeuverTriggers {

    /** Prototype start detector. */
    private final A prototypeStartDetector;

    /** Start detector. */
    private final A startDetector;

    /** Prototype stop detector. */
    private final O prototypeStopDetector;

    /** Stop detector. */
    private final O stopDetector;

    /** Cached field-based start detectors. */
    private final transient Map<Field<? extends CalculusFieldElement<?>>, FieldEventDetector<? extends CalculusFieldElement<?>>> cachedStart;

    /** Cached field-based stop detectors. */
    private final transient Map<Field<? extends CalculusFieldElement<?>>, FieldEventDetector<? extends CalculusFieldElement<?>>> cachedStop;

    /** Simple constructor.
     * <p>
     * Note that the {@code startDetector} and {@code stopDetector} passed as an argument are used only
     * as a <em>prototypes</em> from which new detectors will be built using their
     * {@link AbstractDetector#withHandler(EventHandler) withHandler} methods to
     * set up internal handlers. This implies that the {@link #getStartDetector()} and {@link #getStopDetector()}
     * will <em>not</em> return the objects that were passed as arguments. However,
     * the {@link EventHandler#init(SpacecraftState, AbsoluteDate, EventDetector)
     * init}; {@link EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean)
     * eventOccurred}, and {@link EventHandler#resetState(EventDetector, SpacecraftState)
     * resetState} methods from the prototype event handlers will be called too by
     * the internal handlers corresponding methods. If the prototype {@link
     * EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean) eventOccurred}
     * return {@link Action#CONTINUE}, they will be replaced by {@link Action#RESET_DERIVATIVES,
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
     * @param prototypeStartDetector prototype detector for firing start
     * @param prototypeStopDetector prototype detector for firing stop
     */
    protected StartStopEventsTrigger(final A prototypeStartDetector, final O prototypeStopDetector) {

        this.prototypeStartDetector = prototypeStartDetector;
        this.startDetector          = prototypeStartDetector.
                                      withHandler(new TriggerHandler<>(prototypeStartDetector,
                                                                       this::handleStartFiring));

        this.prototypeStopDetector  = prototypeStopDetector;
        this.stopDetector           = prototypeStopDetector.
                                      withHandler(new TriggerHandler<>(prototypeStopDetector,
                                                                       this::handleStopFiring));

        this.cachedStart = new HashMap<>();
        this.cachedStop  = new HashMap<>();

    }

    /**
     * Getter for the firing start detector.
     * @return firing start detector
     */
    public A getStartDetector() {
        return startDetector;
    }

    /**
     * Getter for the firing stop detector.
     * @return firing stop detector
     */
    public O getStopDetector() {
        return stopDetector;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean isFiringOnInitialState(final SpacecraftState initialState, final boolean isForward) {

        final double startG = startDetector.g(initialState);
        if (startG == 0) {
            // check if we are going to start firing
            final boolean increasing = startDetector.g(initialState.shiftedBy(startDetector.getThreshold())) > 0;
            return increasing && isForward;
        } else if (startG < 0) {
            // we are before start
            return false;
        } else {
            // we are after start
            final double stopG = stopDetector.g(initialState);
            if (stopG == 0) {
                // check if we are going to stop firing
                final boolean increasing = stopDetector.g(initialState.shiftedBy(stopDetector.getThreshold())) > 0;
                return !(increasing && isForward);
            } else if (stopG > 0) {
                // we are after stop
                return false;
            } else {
                // we are between start and stop
                return true;
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventsDetectors() {
        return Stream.of(startDetector, stopDetector);
    }

    /** {@inheritDoc} */
    @Override
    public <S extends CalculusFieldElement<S>> Stream<FieldEventDetector<S>> getFieldEventsDetectors(final Field<S> field) {

        // get the field version of the start detector
        @SuppressWarnings("unchecked")
        FieldEventDetector<S> fStart = (FieldEventDetector<S>) cachedStart.get(field);
        if (fStart == null) {
            fStart = convertAndSetUpStartHandler(field);
            cachedStart.put(field, fStart);
        }

        // get the field version of the stop detector
        @SuppressWarnings("unchecked")
        FieldEventDetector<S> fStop = (FieldEventDetector<S>) cachedStop.get(field);
        if (fStop == null) {
            fStop = convertAndSetUpStopHandler(field);
            cachedStop.put(field, fStop);
        }

        return Stream.of(fStart, fStop);

    }

    /** Convert a detector and set up new handler.
     * <p>
     * This method is not inlined in {@link #getFieldEventsDetectors(Field)} because the
     * parameterized types confuses the Java compiler.
     * </p>
     * @param field field to which the state belongs
     * @param <D> type of the event detector
     * @param <S> type of the field elements
     * @return converted firing intervals detector
     */
    private <D extends FieldAbstractDetector<D, S>, S extends CalculusFieldElement<S>> D convertAndSetUpStartHandler(final Field<S> field) {
        final FieldAbstractDetector<D, S> converted = convertStartDetector(field, prototypeStartDetector);
        return converted.
               withMaxCheck(field.getZero().newInstance(prototypeStartDetector.getMaxCheckInterval())).
               withThreshold(field.getZero().newInstance(prototypeStartDetector.getThreshold())).
               withHandler(new FieldTriggerHandler<>(prototypeStartDetector, this::handleStartFiring));
    }

    /** Convert a detector and set up new handler.
     * <p>
     * This method is not inlined in {@link #getFieldEventsDetectors(Field)} because the
     * parameterized types confuses the Java compiler.
     * </p>
     * @param field field to which the state belongs
     * @param <D> type of the event detector
     * @param <S> type of the field elements
     * @return converted firing intervals detector
     */
    private <D extends FieldAbstractDetector<D, S>, S extends CalculusFieldElement<S>> D convertAndSetUpStopHandler(final Field<S> field) {
        final FieldAbstractDetector<D, S> converted = convertStopDetector(field, prototypeStopDetector);
        return converted.
               withMaxCheck(field.getZero().newInstance(prototypeStopDetector.getMaxCheckInterval())).
               withThreshold(field.getZero().newInstance(prototypeStopDetector.getThreshold())).
               withHandler(new FieldTriggerHandler<>(prototypeStopDetector, this::handleStopFiring));
    }

    /** Convert a primitive firing start detector into a field firing start detector.
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
     *         FieldAbstractDetector<D, S> convertStartDetector(final Field<S> field, final XyzDetector detector) {
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
     * @param detector primitive firing start detector to convert
     * @param <D> type of the event detector
     * @param <S> type of the field elements
     * @return converted firing start detector
     */
    protected abstract <D extends FieldEventDetector<S>, S extends CalculusFieldElement<S>> FieldAbstractDetector<D, S>
        convertStartDetector(Field<S> field, A detector);

    /** Convert a primitive firing stop detector into a field firing stop detector.
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
     *         FieldAbstractDetector<D, S> convertStopDetector(final Field<S> field, final XyzDetector detector) {
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
     * @param detector primitive firing stop detector to convert
     * @param <D> type of the event detector
     * @param <S> type of the field elements
     * @return converted firing stop detector
     */
    protected abstract <D extends FieldEventDetector<S>, S extends CalculusFieldElement<S>> FieldAbstractDetector<D, S>
        convertStopDetector(Field<S> field, O detector);

    /** Handler firing interval start.
     * @param prototypeAction action from the prototype event handler
     * @param state SpaceCraft state to be used in the evaluation
     * @param increasing with the event occurred in an "increasing" or "decreasing" slope direction
     * @param forward indicator for forward propagation
     * @return the Action that the calling detector should pass back to the evaluation system
     */
    private Action handleStartFiring(final Action prototypeAction, final SpacecraftState state,
                                     final boolean increasing, final boolean forward) {
        if (increasing) {
            // the event is meaningful for maneuver firing
            final Action action = prototypeAction == Action.CONTINUE ? Action.RESET_DERIVATIVES : prototypeAction;
            if (forward) {
                getFirings().addValidAfter(true, state.getDate());
            } else {
                getFirings().addValidBefore(false, state.getDate());
            }
            return action;
        } else {
            // the event is not meaningful for maneuver firing
            return prototypeAction;
        }
    }

    /** Handler firing interval stop.
     * @param prototypeAction action from the prototype event handler
     * @param state SpaceCraft state to be used in the evaluation
     * @param increasing with the event occurred in an "increasing" or "decreasing" slope direction
     * @param forward indicator for forward propagation
     * @return the Action that the calling detector should pass back to the evaluation system
     */
    private Action handleStopFiring(final Action prototypeAction, final SpacecraftState state,
                                    final boolean increasing, final boolean forward) {
        if (increasing) {
            // the event is meaningful for maneuver firing
            final Action action = prototypeAction == Action.CONTINUE ? Action.RESET_DERIVATIVES : prototypeAction;
            if (forward) {
                getFirings().addValidAfter(false, state.getDate());
            } else {
                getFirings().addValidBefore(true, state.getDate());
            }
            return action;
        } else {
            // the event is not meaningful for maneuver firing
            return prototypeAction;
        }
    }

    /** Handler firing interval start.
     * @param prototypeAction action from the prototype event handler
     * @param state SpaceCraft state to be used in the evaluation
     * @param increasing with the event occurred in an "increasing" or "decreasing" slope direction
     * @param forward indicator for forward propagation
     * @param <S> type of the field elements
     * @return the Action that the calling detector should pass back to the evaluation system
     */
    private <S extends CalculusFieldElement<S>> Action handleStartFiring(final Action prototypeAction, final FieldSpacecraftState<S> state,
                                                                         final boolean increasing, final boolean forward) {
        if (increasing) {
            // the event is meaningful for maneuver firing
            final Action action = prototypeAction == Action.CONTINUE ? Action.RESET_DERIVATIVES : prototypeAction;
            if (forward) {
                getFirings().addValidAfter(true, state.getDate().toAbsoluteDate());
            } else {
                getFirings().addValidBefore(false, state.getDate().toAbsoluteDate());
            }
            return action;
        } else {
            // the event is not meaningful for maneuver firing
            return prototypeAction;
        }
    }

    /** Handler firing interval stop.
     * @param prototypeAction action from the prototype event handler
     * @param state SpaceCraft state to be used in the evaluation
     * @param increasing with the event occurred in an "increasing" or "decreasing" slope direction
     * @param forward indicator for forward propagation
     * @param <S> type of the field elements
     * @return the Action that the calling detector should pass back to the evaluation system
     */
    private <S extends CalculusFieldElement<S>> Action handleStopFiring(final Action prototypeAction, final FieldSpacecraftState<S> state,
                                                                        final boolean increasing, final boolean forward) {
        if (increasing) {
            // the event is meaningful for maneuver firing
            final Action action = prototypeAction == Action.CONTINUE ? Action.RESET_DERIVATIVES : prototypeAction;
            if (forward) {
                getFirings().addValidAfter(false, state.getDate().toAbsoluteDate());
            } else {
                getFirings().addValidBefore(true, state.getDate().toAbsoluteDate());
            }
            return action;
        } else {
            // the event is not meaningful for maneuver firing
            return prototypeAction;
        }
    }

}
