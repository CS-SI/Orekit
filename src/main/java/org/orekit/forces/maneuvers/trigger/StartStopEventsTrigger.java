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
import org.orekit.time.FieldAbsoluteDate;

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

    /** Start detector. */
    private final A startDetector;

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
     * set up internal handlers. The original event handlers from the prototype
     * will be <em>ignored</em> and never called.
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

        this.startDetector = prototypeStartDetector.withHandler(new StartHandler());
        this.stopDetector  = prototypeStopDetector.withHandler(new StopHandler());
        this.cachedStart   = new HashMap<>();
        this.cachedStop    = new HashMap<>();

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
            final boolean increasing = startDetector.g(initialState.shiftedBy(2 * startDetector.getThreshold())) > 0;
            if (increasing) {
                // we are at maneuver start
                notifyResetters(initialState, true);
                // if propagating forward, we start firing
                return isForward;
            } else {
                // not a meaningful crossing
                return false;
            }
        } else if (startG < 0) {
            // we are before start
            return false;
        } else {
            // we are after start
            final double stopG = stopDetector.g(initialState);
            if (stopG == 0) {
                final boolean increasing = stopDetector.g(initialState.shiftedBy(2 * stopDetector.getThreshold())) > 0;
                if (increasing) {
                    // we are at maneuver end
                    notifyResetters(initialState, false);
                    // if propagating backward, we start firing
                    return !isForward;
                } else {
                    // not a meaningful crossing
                    return false;
                }
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
        final FieldAbstractDetector<D, S> converted = convertStartDetector(field, startDetector);
        return converted.
               withMaxCheck(field.getZero().newInstance(startDetector.getMaxCheckInterval())).
               withThreshold(field.getZero().newInstance(startDetector.getThreshold())).
               withHandler(new FieldStartHandler<>());
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
        final FieldAbstractDetector<D, S> converted = convertStopDetector(field, stopDetector);
        return converted.
               withMaxCheck(field.getZero().newInstance(stopDetector.getMaxCheckInterval())).
               withThreshold(field.getZero().newInstance(stopDetector.getThreshold())).
               withHandler(new FieldStopHandler<>());
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
     *         final FieldAbstractDetector<D, S> converted = (FieldAbstractDetector<D, S>) new FieldXyzDetector<>(date, param);
     *         return converted;
     *
     *     }
     * }
     * </pre>
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
     *         final FieldAbstractDetector<D, S> converted = (FieldAbstractDetector<D, S>) new FieldXyzDetector<>(date, param);
     *         return converted;
     *
     *     }
     * }
     * </pre>
     * @param field field to which the state belongs
     * @param detector primitive firing stop detector to convert
     * @param <D> type of the event detector
     * @param <S> type of the field elements
     * @return converted firing stop detector
     */
    protected abstract <D extends FieldEventDetector<S>, S extends CalculusFieldElement<S>> FieldAbstractDetector<D, S>
        convertStopDetector(Field<S> field, O detector);

    /** Local handler for start triggers. */
    private class StartHandler implements EventHandler<A> {

        /** Propagation direction. */
        private boolean forward;

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState initialState, final AbsoluteDate target, final A detector) {
            forward = target.isAfterOrEqualTo(initialState);
            initializeResetters(initialState, target);
        }

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final SpacecraftState s, final A detector, final boolean increasing) {
            if (increasing) {
                // the event is meaningful for maneuver firing
                if (forward) {
                    getFirings().addValidAfter(true, s.getDate());
                } else {
                    getFirings().addValidBefore(false, s.getDate());
                }
                notifyResetters(s, true);
                return Action.RESET_STATE;
            } else {
                // the event is not meaningful for maneuver firing
                return Action.CONTINUE;
            }
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final A detector, final SpacecraftState oldState) {
            return applyResetters(oldState);
        }

    }

    /** Local handler for stop triggers. */
    private class StopHandler implements EventHandler<O> {

        /** Propagation direction. */
        private boolean forward;

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState initialState, final AbsoluteDate target, final O detector) {
            forward = target.isAfterOrEqualTo(initialState);
            initializeResetters(initialState, target);
        }

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final SpacecraftState s, final O detector, final boolean increasing) {
            if (increasing) {
                // the event is meaningful for maneuver firing
                if (forward) {
                    getFirings().addValidAfter(false, s.getDate());
                } else {
                    getFirings().addValidBefore(true, s.getDate());
                }
                notifyResetters(s, false);
                return Action.RESET_STATE;
            } else {
                // the event is not meaningful for maneuver firing
                return Action.CONTINUE;
            }
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final O detector, final SpacecraftState oldState) {
            return applyResetters(oldState);
        }

    }

    /** Local handler for start triggers.
     * @param <S> type of the field elements
     */
    private class FieldStartHandler<D extends FieldEventDetector<S>, S extends CalculusFieldElement<S>> implements FieldEventHandler<D, S> {

        /** Propagation direction. */
        private boolean forward;

        /** {@inheritDoc} */
        @Override
        public void init(final FieldSpacecraftState<S> initialState, final FieldAbsoluteDate<S> target) {
            forward = target.isAfterOrEqualTo(initialState);
            initializeResetters(initialState, target);
        }

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final FieldSpacecraftState<S> s, final D detector, final boolean increasing) {
            if (increasing) {
                // the event is meaningful for maneuver firing
                if (forward) {
                    getFirings().addValidAfter(true, s.getDate().toAbsoluteDate());
                } else {
                    getFirings().addValidBefore(false, s.getDate().toAbsoluteDate());
                }
                notifyResetters(s, true);
                return Action.RESET_STATE;
            } else {
                // the event is not meaningful for maneuver firing
                return Action.CONTINUE;
            }
        }

        /** {@inheritDoc} */
        @Override
        public FieldSpacecraftState<S> resetState(final D detector, final FieldSpacecraftState<S> oldState) {
            return applyResetters(oldState);
        }

    }

    /** Local handler for stop triggers.
     * @param <S> type of the field elements
     */
    private class FieldStopHandler<D extends FieldEventDetector<S>, S extends CalculusFieldElement<S>> implements FieldEventHandler<D, S> {

        /** Propagation direction. */
        private boolean forward;

        /** {@inheritDoc} */
        @Override
        public void init(final FieldSpacecraftState<S> initialState, final FieldAbsoluteDate<S> target) {
            forward = target.isAfterOrEqualTo(initialState);
            initializeResetters(initialState, target);
        }

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final FieldSpacecraftState<S> s, final D detector, final boolean increasing) {
            if (increasing) {
                // the event is meaningful for maneuver firing
                if (forward) {
                    getFirings().addValidAfter(false, s.getDate().toAbsoluteDate());
                } else {
                    getFirings().addValidBefore(true, s.getDate().toAbsoluteDate());
                }
                notifyResetters(s, false);
                return Action.RESET_STATE;
            } else {
                // the event is not meaningful for maneuver firing
                return Action.CONTINUE;
            }
        }

        /** {@inheritDoc} */
        @Override
        public FieldSpacecraftState<S> resetState(final D detector, final FieldSpacecraftState<S> oldState) {
            return applyResetters(oldState);
        }

    }

}
