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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.events.Action;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.TimeSpanMap;

/** Base class for triggers.
 * @author Luc Maisonobe
 * @since 11.1
 */
public abstract class AbstractManeuverTriggers implements ResettableManeuverTriggers {

    /** Firing time spans. */
    private TimeSpanMap<Boolean> firings;

    /** Propagation direction. */
    private boolean forward;

    /** Resetters for the maneuver triggers. */
    private final List<ManeuverTriggersResetter> resetters;

    /** Cached field-based resetters. */
    private final Map<Field<? extends CalculusFieldElement<?>>, List<FieldManeuverTriggersResetter<?>>> fieldResetters;

    /** Simple constructor.
     */
    protected AbstractManeuverTriggers() {
        this.firings        = new TimeSpanMap<>(Boolean.FALSE);
        this.resetters      = new ArrayList<>();
        this.fieldResetters = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {

        forward = target.isAfterOrEqualTo(initialState);
        firings = new TimeSpanMap<>(Boolean.FALSE);
        initializeResetters(initialState, target);

        if (isFiringOnInitialState(initialState, forward)) {
            if (forward) {
                firings.addValidAfter(Boolean.TRUE, initialState.getDate(), false);
            } else {
                firings.addValidBefore(Boolean.TRUE, initialState.getDate(), false);
            }
        }

    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends CalculusFieldElement<T>> void init(final FieldSpacecraftState<T> initialState, final FieldAbsoluteDate<T> target) {

        forward = target.isAfterOrEqualTo(initialState);
        firings = new TimeSpanMap<>(Boolean.FALSE);
        // check if we already have resetters for this field
        final List<FieldManeuverTriggersResetter<?>> list = fieldResetters.get(initialState.getDate().getField());
        if (list != null) {
            for (FieldManeuverTriggersResetter<?> r : list) {
                ((FieldManeuverTriggersResetter<T>) r).init(initialState, target);
            }
        }

        if (isFiringOnInitialState(initialState.toSpacecraftState(), forward)) {
            if (forward) {
                firings.addValidAfter(Boolean.TRUE, initialState.getDate().toAbsoluteDate(), false);
            } else {
                firings.addValidBefore(Boolean.TRUE, initialState.getDate().toAbsoluteDate(), false);
            }
        }

    }

    /**
     * Method to check if the thruster is firing on initialization. can be called by
     * sub classes
     *
     * @param initialState initial spacecraft state
     * @param isForward if true, propagation will be in the forward direction
     * @return true if firing in propagation direction
     */
    protected abstract boolean isFiringOnInitialState(SpacecraftState initialState, boolean isForward);

    /** {@inheritDoc} */
    @Override
    public boolean isFiring(final AbsoluteDate date, final double[] parameters) {
        return firings.get(date);
    }

    /** {@inheritDoc} */
    @Override
    public <S extends CalculusFieldElement<S>> boolean isFiring(final FieldAbsoluteDate<S> date, final S[] parameters) {
        return firings.get(date.toAbsoluteDate());
    }

    /** Get the firings detected during last propagation.
     * @return firings detected during last propagation
     */
    public TimeSpanMap<Boolean> getFirings() {
        return firings;
    }

    /** {@inheritDoc} */
    @Override
    public void addResetter(final ManeuverTriggersResetter resetter) {
        resetters.add(resetter);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> void addResetter(final Field<T> field, final FieldManeuverTriggersResetter<T> resetter) {

        // check if we already have resetters for this field
        final List<FieldManeuverTriggersResetter<?>> list = fieldResetters.computeIfAbsent(field, k -> new ArrayList<>());

        // add the resetter to the list
        list.add(resetter);

    }

    /** Initialize resetters.
     * @param initialState initial state
     * @param target target date for the propagation
     */
    protected void initializeResetters(final SpacecraftState initialState, final AbsoluteDate target) {
        for (final ManeuverTriggersResetter r : resetters) {
            r.init(initialState, target);
        }
    }

    /** Notify resetters.
     * @param state spacecraft state at trigger date (before applying the maneuver)
     * @param start if true, the trigger is the start of the maneuver
     */
    protected void notifyResetters(final SpacecraftState state, final boolean start) {
        for (final ManeuverTriggersResetter r : resetters) {
            r.maneuverTriggered(state, start);
        }
    }

    /** Apply resetters.
     * @param state spacecraft state at trigger date
     * @return reset state
     */
    protected SpacecraftState applyResetters(final SpacecraftState state) {
        SpacecraftState reset = state;
        for (final ManeuverTriggersResetter r : resetters) {
            reset = r.resetState(reset);
        }
        return reset;
    }

    /** Initialize resetters.
     * @param initialState initial state
     * @param target target date for the propagation
     * @param <T> type of the field elements
     */
    protected <T extends CalculusFieldElement<T>> void initializeResetters(final FieldSpacecraftState<T> initialState, final FieldAbsoluteDate<T> target) {
        final List<FieldManeuverTriggersResetter<?>> list = fieldResetters.get(initialState.getDate().getField());
        if (list != null) {
            for (final FieldManeuverTriggersResetter<?> r : list) {
                @SuppressWarnings("unchecked")
                final FieldManeuverTriggersResetter<T> tr = (FieldManeuverTriggersResetter<T>) r;
                tr.init(initialState, target);
            }
        }
    }

    /** Notify resetters.
     * @param state spacecraft state at trigger date (before applying the maneuver)
     * @param start if true, the trigger is the start of the maneuver
     * @param <T> type of the field elements
     */
    protected <T extends CalculusFieldElement<T>> void notifyResetters(final FieldSpacecraftState<T> state, final boolean start) {
        final List<FieldManeuverTriggersResetter<?>> list = fieldResetters.get(state.getDate().getField());
        if (list != null) {
            for (final FieldManeuverTriggersResetter<?> r : list) {
                @SuppressWarnings("unchecked")
                final FieldManeuverTriggersResetter<T> tr = (FieldManeuverTriggersResetter<T>) r;
                tr.maneuverTriggered(state, start);
            }
        }
    }

    /** Apply resetters.
     * @param state spacecraft state at trigger date
     * @param <T> type of the field elements
     * @return reset state
     */
    protected <T extends CalculusFieldElement<T>> FieldSpacecraftState<T>
        applyResetters(final FieldSpacecraftState<T> state) {
        FieldSpacecraftState<T> reset = state;
        final List<FieldManeuverTriggersResetter<?>> list = fieldResetters.get(state.getDate().getField());
        if (list != null) {
            for (final FieldManeuverTriggersResetter<?> r : list) {
                @SuppressWarnings("unchecked")
                final FieldManeuverTriggersResetter<T> tr = (FieldManeuverTriggersResetter<T>) r;
                reset = tr.resetState(reset);
            }
        }
        return reset;
    }

    /** Local abstract handler for triggers, with a cache for the reset.
     * @since 13.1
     */
    protected abstract class TriggerHandler implements EventHandler {

        /** Propagation direction. */
        private boolean forward;

        /** Last evaluated state for cache. */
        private SpacecraftState lastState;

        /** Last reset state for cache. */
        private SpacecraftState lastResetState;

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState initialState, final AbsoluteDate target, final EventDetector detector) {
            forward = target.isAfterOrEqualTo(initialState);
            lastState = null;
            lastResetState = null;
            initializeResetters(initialState, target);
        }

        /**
         * Determines the action (reset state or derivatives only).
         * @param detector event detector
         * @param oldState state before reset if any
         * @return action
         */
        protected Action determineAction(final EventDetector detector, final SpacecraftState oldState) {
            final SpacecraftState resetState = resetState(detector, oldState);
            if (resetState == oldState) {
                return Action.RESET_DERIVATIVES;
            } else {
                return Action.RESET_STATE;
            }
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final EventDetector detector, final SpacecraftState oldState) {
            if (lastState != oldState) {
                lastResetState = applyResetters(oldState);
                lastState = oldState;
            }
            return lastResetState;
        }

        /**
         * Getter for flag.
         * @return flag on backward propagation
         */
        protected boolean isForward() {
            return forward;
        }
    }

    /** Local abstract handler for triggers, with a cache for the reset.
     * @param <S> type of the field elements
     * @since 13.1
     */
    protected abstract class FieldTriggerHandler<S extends CalculusFieldElement<S>> implements FieldEventHandler<S> {

        /** Propagation direction. */
        private boolean forward;

        /** Last evaluated state for cache. */
        private FieldSpacecraftState<S> lastState;

        /** Last reset state for cache. */
        private FieldSpacecraftState<S> lastResetState;

        /** {@inheritDoc} */
        @Override
        public void init(final FieldSpacecraftState<S> initialState,
                         final FieldAbsoluteDate<S> target,
                         final FieldEventDetector<S> detector) {
            forward = target.isAfterOrEqualTo(initialState);
            lastState = null;
            lastResetState = null;
            initializeResetters(initialState, target);
        }

        /**
         * Determines the action (reset state or derivatives only).
         * @param detector event detector
         * @param oldState state before reset if any
         * @return action
         */
        protected Action determineAction(final FieldEventDetector<S> detector, final FieldSpacecraftState<S> oldState) {
            final FieldSpacecraftState<S> resetState = resetState(detector, oldState);
            if (resetState == oldState) {
                return Action.RESET_DERIVATIVES;
            } else {
                return Action.RESET_STATE;
            }
        }

        /** {@inheritDoc} */
        @Override
        public FieldSpacecraftState<S> resetState(final FieldEventDetector<S> detector, final FieldSpacecraftState<S> oldState) {
            if (lastState != oldState) {
                lastResetState = applyResetters(oldState);
                lastState = oldState;
            }
            return lastResetState;
        }

        /**
         * Getter for flag.
         * @return flag on backward propagation
         */
        protected boolean isForward() {
            return forward;
        }
    }
}
